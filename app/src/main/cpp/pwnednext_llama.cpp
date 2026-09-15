#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <cstdarg>
#include <mutex>
#include <string>
#include <vector>

#include <unistd.h>

#include "llama.h"

namespace {

constexpr char LOG_TAG[] = "PwnedNextLlama";
constexpr int32_t CONTEXT_SIZE = 1024;
constexpr int32_t BATCH_SIZE = 128;
constexpr int32_t MAX_THREADS = 8;
constexpr int32_t MAX_THREAD_HEADROOM = 0;

struct Engine {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    llama_adapter_lora * adapter = nullptr;
};

std::once_flag backend_initialized;

void logError(const char * message) {
    __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, message);
}

void logInfo(const char * format, ...) {
    va_list arguments;
    va_start(arguments, format);
    __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, format, arguments);
    va_end(arguments);
}

std::string jstringToString(JNIEnv * env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char * chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars == nullptr ? "" : chars);
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}

std::string tokenToPiece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(256);
    int32_t length = llama_token_to_piece(
            vocab,
            token,
            buffer.data(),
            static_cast<int32_t>(buffer.size()),
            0,
            false);
    if (length < 0) {
        buffer.resize(static_cast<size_t>(-length));
        length = llama_token_to_piece(
                vocab,
                token,
                buffer.data(),
                static_cast<int32_t>(buffer.size()),
                0,
                false);
    }
    if (length <= 0) {
        return {};
    }
    return std::string(buffer.data(), static_cast<size_t>(length));
}

bool decodePrompt(Engine * engine, const std::vector<llama_token> & tokens) {
    if (tokens.empty() || tokens.size() >= CONTEXT_SIZE) {
        return false;
    }
    for (size_t offset = 0; offset < tokens.size(); offset += BATCH_SIZE) {
        const size_t remaining = tokens.size() - offset;
        const int32_t batch_size = static_cast<int32_t>(
                std::min<size_t>(remaining, BATCH_SIZE));
        llama_batch batch = llama_batch_init(BATCH_SIZE, 0, 1);
        batch.n_tokens = batch_size;
        for (int32_t index = 0; index < batch.n_tokens; ++index) {
            const size_t token_index = offset + static_cast<size_t>(index);
            batch.token[index] = tokens[token_index];
            batch.pos[index] = static_cast<llama_pos>(token_index);
            batch.n_seq_id[index] = 1;
            batch.seq_id[index][0] = 0;
            batch.logits[index] = token_index == tokens.size() - 1;
        }
        const int32_t result = llama_decode(engine->context, batch);
        llama_batch_free(batch);
        if (result != 0) {
            return false;
        }
    }
    return true;
}

std::string generate(Engine * engine, const std::string & prompt, int32_t max_tokens) {
    const llama_vocab * vocab = llama_model_get_vocab(engine->model);
    const int32_t required_tokens = llama_tokenize(
            vocab,
            prompt.c_str(),
            static_cast<int32_t>(prompt.size()),
            nullptr,
            0,
            true,
            true);
    if (required_tokens >= 0) {
        logError("Prompt tokenization did not report a required buffer size");
        return {};
    }

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(-required_tokens));
    logInfo("Tokenized prompt into %d tokens", -required_tokens);
    if (llama_tokenize(
                vocab,
                prompt.c_str(),
                static_cast<int32_t>(prompt.size()),
                prompt_tokens.data(),
                static_cast<int32_t>(prompt_tokens.size()),
                true,
                true) < 0) {
        return {};
    }

    llama_memory_clear(llama_get_memory(engine->context), true);
    logInfo("Decoding prompt");
    if (!decodePrompt(engine, prompt_tokens)) {
        logError("Prompt decode failed");
        return {};
    }
    logInfo("Prompt decoded; generating up to %d tokens", max_tokens);

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string output;
    const llama_token * last_token = nullptr;
    for (int32_t index = 0; index < max_tokens; ++index) {
        const llama_token token = llama_sampler_sample(sampler, engine->context, -1);
        if (llama_vocab_is_eog(vocab, token)) {
            logInfo("Generation reached end-of-generation token after %d tokens", index);
            break;
        }
        output += tokenToPiece(vocab, token);
        const size_t statement_end = output.find(';');
        if (statement_end != std::string::npos) {
            output.resize(statement_end);
            break;
        }
        llama_sampler_accept(sampler, token);

        llama_batch batch = llama_batch_init(1, 0, 1);
        batch.n_tokens = 1;
        batch.token[0] = token;
        batch.pos[0] = static_cast<llama_pos>(prompt_tokens.size() + index);
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        if (llama_decode(engine->context, batch) != 0) {
            llama_batch_free(batch);
            break;
        }
        llama_batch_free(batch);
        last_token = &token;
    }
    (void) last_token;
    llama_sampler_free(sampler);
    logInfo("Generation completed with %zu output bytes", output.size());
    return output;
}

}  // namespace

extern "C"
JNIEXPORT jlong JNICALL
Java_org_owasp_pwnednext_android_sql_EmbeddedLlamaSqlModel_nativeCreate(
        JNIEnv * env,
        jclass,
        jstring model_path,
        jstring adapter_path) {
    std::call_once(backend_initialized, []() {
        llama_backend_init();
    });

    const std::string model_file = jstringToString(env, model_path);
    const std::string adapter_file = jstringToString(env, adapter_path);
    logInfo("Loading GGUF model");
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    Engine * engine = new Engine();
    engine->model = llama_model_load_from_file(model_file.c_str(), model_params);
    if (engine->model == nullptr) {
        logError("Unable to load the embedded GGUF model");
        delete engine;
        return 0;
    }
    logInfo("GGUF model loaded");

    if (!adapter_file.empty()) {
        logInfo("Loading optional LoRA adapter");
        engine->adapter = llama_adapter_lora_init(engine->model, adapter_file.c_str());
        if (engine->adapter == nullptr) {
            logError("Unable to load the optional GGUF adapter; continuing without it");
        } else {
            logInfo("Optional LoRA adapter loaded");
        }
    }

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = CONTEXT_SIZE;
    context_params.n_batch = BATCH_SIZE;
    context_params.n_ubatch = BATCH_SIZE;
    const long available_threads = sysconf(_SC_NPROCESSORS_ONLN);
    context_params.n_threads = static_cast<int32_t>(
            std::max(2L, std::min(static_cast<long>(MAX_THREADS),
                                  available_threads - MAX_THREAD_HEADROOM)));
    context_params.n_threads_batch = context_params.n_threads;
    engine->context = llama_init_from_model(engine->model, context_params);
    if (engine->context == nullptr) {
        logError("Unable to create the llama.cpp context");
        if (engine->adapter != nullptr) {
            llama_adapter_lora_free(engine->adapter);
        }
        llama_model_free(engine->model);
        delete engine;
        return 0;
    }
    logInfo("llama.cpp context created with %d threads", context_params.n_threads);

    if (engine->adapter != nullptr) {
        llama_adapter_lora * adapters[] = {engine->adapter};
        float scales[] = {1.0f};
        if (llama_set_adapters_lora(engine->context, adapters, 1, scales) != 0) {
            logError("Unable to apply the optional GGUF adapter; continuing without it");
            llama_adapter_lora_free(engine->adapter);
            engine->adapter = nullptr;
        } else {
            logInfo("Optional LoRA adapter applied");
        }
    }
    return reinterpret_cast<jlong>(engine);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_owasp_pwnednext_android_sql_EmbeddedLlamaSqlModel_nativeGenerate(
        JNIEnv * env,
        jclass,
        jlong handle,
        jstring prompt,
        jint max_tokens) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine == nullptr || engine->context == nullptr) {
        return nullptr;
    }
    const std::string output = generate(engine, jstringToString(env, prompt), max_tokens);
    return env->NewStringUTF(output.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_org_owasp_pwnednext_android_sql_EmbeddedLlamaSqlModel_nativeDestroy(
        JNIEnv *,
        jclass,
        jlong handle) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine == nullptr) {
        return;
    }
    if (engine->context != nullptr) {
        llama_free(engine->context);
    }
    if (engine->adapter != nullptr) {
        llama_adapter_lora_free(engine->adapter);
    }
    if (engine->model != nullptr) {
        llama_model_free(engine->model);
    }
    delete engine;
}
