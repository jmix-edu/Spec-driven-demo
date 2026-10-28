package com.company.booking.service;

import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deliberately NOT authenticated: seedIfEmpty() runs on the startup thread, which has no user,
 * so the calls under test must work without authentication.
 */
@SpringBootTest
@ActiveProfiles("test")
public class DemoDataServiceTest {

    @Autowired
    DemoDataService demoDataService;

    @Autowired
    DataManager dataManager;

    @Autowired
    SystemAuthenticator systemAuthenticator;

    @Test
    void seedIfEmpty_worksWithoutAuthenticationAndIsIdempotent() {
        // the startup listener has already run; this call either seeds or finds data, but must not throw
        demoDataService.seedIfEmpty();
        long buildings = count("select count(e) from Building e");
        long desks = count("select count(e) from Desk e");

        boolean seededAgain = demoDataService.seedIfEmpty();

        assertThat(seededAgain).isFalse();
        assertThat(count("select count(e) from Building e")).isEqualTo(buildings);
        assertThat(count("select count(e) from Desk e")).isEqualTo(desks);
    }

    private long count(final String query) {
        // only the assertion reads are authenticated, never the calls under test
        return systemAuthenticator.withSystem(() -> dataManager.loadValue(query, Long.class).one());
    }
}
