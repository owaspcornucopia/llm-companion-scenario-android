package org.owasp.pwnednext.android.scenario;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public final class MobileThreatSurfaceTest {
    @Test
    public void exposesRawClipboardAndProviderBoundaries() {
        assertEquals("rows and memo", TheOldPawnedNextSurface.clipboardPayload("rows and memo"));
        assertEquals(
                "SELECT * FROM transactions",
                TheOldPawnedNextSurface.rawProviderSql(null));
        assertEquals(
                "SELECT * FROM transactions WHERE 1=1",
                TheOldPawnedNextSurface.rawProviderSql("1=1"));
        assertEquals("files/reports/../../databases/pwnednext.db",
                TheOldPawnedNextSurface.traversalTarget("../../databases/pwnednext.db"));
    }

    @Test
    public void acceptsClientStateAndSkipsStepUp() {
        assertFalse(TheOldPawnedNextSurface.requiresStepUp(12500.00));
        assertTrue(TheOldPawnedNextSurface.acceptsClientAuthorization(null));
        assertTrue(TheOldPawnedNextSurface.acceptsClientAuthorization(true));
        assertFalse(TheOldPawnedNextSurface.acceptsClientAuthorization(false));
        assertTrue(TheOldPawnedNextSurface.acceptsReplayToken("old-token"));
        assertFalse(TheOldPawnedNextSurface.acceptsReplayToken(""));
        assertTrue(TheOldPawnedNextSurface.acceptsOverride(true));
        assertFalse(TheOldPawnedNextSurface.acceptsOverride(false));
    }

    @Test
    public void reportsUnprotectedRuntimeSurfaces() {
        assertEquals("snapshot", TheOldPawnedNextSurface.debugSnapshot("snapshot"));
        assertEquals("retained", TheOldPawnedNextSurface.retainRawText("retained"));
        assertEquals(6, TheOldPawnedNextSurface.PERMISSIONS.size());
    }

    @Test
    public void rejectsMissingEvidence() {
        assertThrows(IllegalArgumentException.class, () -> TheOldPawnedNextSurface.clipboardPayload(""));
        assertThrows(IllegalArgumentException.class, () -> TheOldPawnedNextSurface.debugSnapshot(null));
        assertThrows(IllegalArgumentException.class, () -> TheOldPawnedNextSurface.retainRawText(""));
        assertThrows(IllegalArgumentException.class, () -> TheOldPawnedNextSurface.traversalTarget(null));
        assertFalse(TheOldPawnedNextSurface.acceptsReplayToken(null));
    }
}
