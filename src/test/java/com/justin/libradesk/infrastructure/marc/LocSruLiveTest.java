package com.justin.libradesk.infrastructure.marc;

import com.justin.libradesk.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live check against the real Library of Congress SRU service. Disabled unless
 * {@code LOC_LIVE=true}, so normal/CI builds stay offline and deterministic; the
 * offline {@link LocSruClientTest} (recorded fixture) is the everyday coverage.
 */
@EnabledIfEnvironmentVariable(named = "LOC_LIVE", matches = "true")
class LocSruLiveTest {

    @Test
    void searchesLibraryOfCongressByIsbn() {
        LocSruClient client = new LocSruClient(new MarcService(), AppConfig.load());

        List<MarcData> results = client.searchByIsbn("9780134685991");

        assertFalse(results.isEmpty(), "expected at least one LoC record");
        assertTrue(results.get(0).book().getTitle() != null, "record should have a title");
    }
}
