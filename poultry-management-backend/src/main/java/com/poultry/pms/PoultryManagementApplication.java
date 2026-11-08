package com.poultry.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Poultry Management System - educational prototype (Kenchic case study).
 * NOT an official Kenchic system and NOT a production veterinary/compliance platform.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PoultryManagementApplication {

    public static void main(String[] args) {
        createDataDirectory();
        SpringApplication.run(PoultryManagementApplication.class, args);
    }

    /** SQLite creates the database FILE but not its folder, so make sure ./data exists before connecting. */
    private static void createDataDirectory() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the ./data folder for the SQLite database", e);
        }
    }
}
