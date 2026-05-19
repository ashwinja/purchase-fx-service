package com.example.purchasefx;

import com.example.purchasefx.config.AppConfig;

/** Unit test for environment-based Java property-file configuration loading. */
public final class AppConfigTest {
    /** Verifies that the test property file overlays the shared defaults. */
    public static void main(String[] args) {
        System.setProperty("app.env", "test");
        try {
            var config = AppConfig.load();
            if (!"test".equals(config.environment())) throw new AssertionError("environment");
            if (config.serverPort() != 0) throw new AssertionError("server.port");
            if (!config.purchaseStorageFile().toString().contains("build")) throw new AssertionError("purchase.storage.file");
            if (!config.treasuryApiBaseUrl().contains("fiscaldata.treasury.gov")) throw new AssertionError("treasury.api.base-url");
            System.out.println("AppConfigTest passed");
        } finally {
            System.clearProperty("app.env");
        }
    }
}
