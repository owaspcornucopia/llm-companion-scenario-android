# On-device model inference

AI Anti Fraud 3.0 has one inference path: the TinyLlama GGUF runs inside the
Android application process through llama.cpp and JNI. There is no Python
service, host endpoint, network model, deterministic heuristic, or fallback
provider.

## Build the APK

Initialise the native dependency and install the Android toolchain:

```powershell
git submodule update --init --recursive
sdkmanager "platforms;android-35" "build-tools;35.0.0" `
  "ndk;26.3.11579264" "cmake;3.31.6"
.\scripts\download-model.ps1
.\scripts\build-embedded-apk.ps1 -SkipModelDownload
```

Every APK build requires
`models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf`. Gradle copies the ignored model
into generated assets and packages it in `app-debug.apk`. If
`models/pwnednext-tinyllama-lora.gguf` exists, it can be packaged by building
with `-PembedLora=true`. The base model is the default because it is the
verified emulator configuration.

The first investigation copies the packaged assets into app-private storage,
loads the model, decodes the prompt, and generates SQL. These operations have no
application-level timeout. A model load, inference, or output-validation failure
is shown as an error instead of switching to another implementation.

## Optional LoRA conversion

The download script retrieves the text-to-SQL adapter source files in
`safetensors` format. Android llama.cpp requires a GGUF adapter. Convert it with
the conversion script from [llama.cpp](https://github.com/ggml-org/llama.cpp):

```powershell
$llamaCpp = Join-Path $env:TEMP "llama.cpp-lora-conversion"
git clone --depth 1 https://github.com/ggml-org/llama.cpp.git $llamaCpp
python -m pip install gguf torch transformers safetensors
python (Join-Path $llamaCpp "convert_lora_to_gguf.py") `
  --outfile .\models\pwnednext-tinyllama-lora.gguf `
  --outtype f16 `
  .\models\text-to-sql-tinyllama-lora
```

The adapter is optional. The base GGUF is always
required and always remains inside the APK and Android app process.
