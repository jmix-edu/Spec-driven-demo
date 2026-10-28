package com.company.booking.listener;

import com.company.booking.service.DemoDataService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data on application startup - in the dev profile only; production and tests start without
 * demo data. The work and its authentication live in {@link DemoDataService#seedIfEmpty()}, called through
 * the Spring proxy.
 */
@Profile("dev")
@Component
public class DemoDataInitializer {

    private final DemoDataService demoDataService;

    public DemoDataInitializer(final DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        demoDataService.seedIfEmpty();
    }
}
