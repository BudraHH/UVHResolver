package com.budra.uvh.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;


@ApplicationPath("/api")
public class AppConfig extends ResourceConfig {
    public AppConfig() {
        // *** FIX: Scan ALL relevant packages for JAX-RS and DI annotations ***
        packages(
                "com.budra.uvh.controllers.requestHandler", // Where RequestHandler (@Path) is
                "com.budra.uvh.service",        // Where LskResolution (@RequestScoped) is
                "com.budra.uvh.model ",         // Where LskCounterRepository (@ApplicationScoped) is
                "com.budra.uvh.exception"                 // Scan for potential ExceptionMappers later if needed
        );
        System.out.println("JAX-RS (Jersey) Application Initialized - Scanning relevant packages.");
    }
}