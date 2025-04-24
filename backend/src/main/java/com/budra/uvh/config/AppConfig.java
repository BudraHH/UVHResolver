package com.budra.uvh.config;

import com.budra.uvh.controllers.RequestHandler; // Ensure correct package location
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
// Imports needed for the binder and HK2's scope:
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped; // <<< IMPORT HK2's Request Scope

// Removed import jakarta.enterprise.context.RequestScoped; // Standard CDI scope no longer used in .in()

@ApplicationPath("/api")
public class AppConfig extends ResourceConfig {
    public AppConfig() {
        // Scan package containing JAX-RS resource classes (@Path)
        // This allows Jersey to discover the RequestHandler endpoint
        packages("com.budra.uvh.controllers");

        // Optional: Scan exception package if you have ExceptionMappers
        // packages("com.budra.uvh.exception");

        System.out.println("Initializing AppConfig - Configuring Manual DI Binding...");

        // Register the AbstractBinder to configure our factory binding
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                System.out.println("Binding ManualDIProviderFactory.RequestHandlerProvider to RequestHandler using HK2 RequestScope...");

                // --- CORRECTED BINDING ---
                // Assumes ManualDIProviderFactory.RequestHandlerProvider implements java.util.function.Supplier
                bindFactory(ManualDIProviderFactory.RequestHandlerProvider.class)
                        .to(RequestHandler.class)
                        // Use HK2's internal RequestScoped class for the scope context
                        // This should resolve the "Could not find an active context" error
                        .in(RequestScoped.class); // <<< USES org.glassfish.jersey.process.internal.RequestScoped
            }
        });

        System.out.println("JAX-RS (Jersey) Application Initialized - MANUAL DI WIRED VIA FACTORY.");
    }
}