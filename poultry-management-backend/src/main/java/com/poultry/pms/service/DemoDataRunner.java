package com.poultry.pms.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Only active when poultry.demo-data.enabled=true. Loads demo data at startup if it was never loaded before. */
@Component
@ConditionalOnProperty(name = "poultry.demo-data.enabled", havingValue = "true")
public class DemoDataRunner implements ApplicationRunner {

    private final DemoDataService demoDataService;

    public DemoDataRunner(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!demoDataService.isLoaded()) {
            demoDataService.seed();
        }
    }
}
