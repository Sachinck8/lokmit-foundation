package com.lokmit.foundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the LOKMIT FOUNDATION backend.
 *
 * <p>Phase 1: application skeleton only. Business modules are added in later phases.</p>
 */
@SpringBootApplication
@EnableScheduling
public class LokmitFoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LokmitFoundationApplication.class, args);
    }
}