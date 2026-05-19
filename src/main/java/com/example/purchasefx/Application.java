package com.example.purchasefx;

import com.example.purchasefx.api.PurchaseHttpHandler;
import com.example.purchasefx.config.AppConfig;
import com.example.purchasefx.infra.FilePurchaseRepository;
import com.example.purchasefx.infra.TreasuryFiscalDataClient;
import com.example.purchasefx.service.PurchaseService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Application entry point for the Purchase FX service.
 *
 * <p>The service intentionally uses only the Java standard library so a reviewer can run it
 * from the command line without installing Gradle, an IDE, a database, Tomcat, or Jetty.
 * Configuration is loaded from {@code application.properties} and the selected
 * {@code application-{env}.properties} file. Use {@code -Dapp.env=dev|test|prod}
 * to select an environment and standard {@code -Dproperty=value} JVM overrides
 * for deployment-specific settings.</p>
 */
public final class Application {
    private Application() {
        // Utility-style class: all behavior starts from main().
    }

    /**
     * Wires the repository, Treasury client, domain service, and HTTP handler, then starts
     * the embedded JDK HTTP server.
     */
    public static void main(String[] args) throws Exception {
        var config = AppConfig.load();
        int port = config.serverPort();

        var repository = new FilePurchaseRepository(config.purchaseStorageFile());
        var treasuryClient = new TreasuryFiscalDataClient(config.treasuryApiBaseUrl());
        var purchaseService = new PurchaseService(repository, treasuryClient);

        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/purchases", new PurchaseHttpHandler(purchaseService));
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
        server.start();

        System.out.printf("Purchase FX service started on http://localhost:%d using app.env=%s%n", port, config.environment());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
    }
}
