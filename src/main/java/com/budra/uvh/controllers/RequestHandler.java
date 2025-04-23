package com.budra.uvh.controllers;

// Correct import for your service class
import com.budra.uvh.service.LskResolution; // Ensure this package is correct

// Import standard JAX-RS annotations
import jakarta.ws.rs.*; // Keep these
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Import your custom exceptions
import com.budra.uvh.exception.LskGenerationException;
import com.budra.uvh.exception.PlaceholderFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/logical-seed-key") // KEEP: JAX-RS annotation for routing
// NO @RequestScoped annotation - Lifecycle managed manually or by factory
public class RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    // Dependency field - made final, initialized by constructor
    private final LskResolution lskResolution;

    // NO @Inject annotation

    // --- Constructor for Manual DI ---
    // This constructor MUST be called by whatever mechanism creates RequestHandler
    // (e.g., the ManualDIProviderFactory shown previously)
    public RequestHandler(LskResolution lskResolution) {
        log.debug("RequestHandler instance MANUALLY created via constructor.");
        if (lskResolution == null) {
            // Fail fast if the dependency wasn't provided during manual wiring
            throw new IllegalArgumentException("LskResolution cannot be null for RequestHandler");
        }
        this.lskResolution = lskResolution;
    }

    // Default no-arg constructor REMOVED - it's not used by the manual factory approach


    @POST // KEEP: JAX-RS annotation
    @Path("/resolve") // KEEP: JAX-RS annotation
    @Consumes(MediaType.APPLICATION_XML) // KEEP: JAX-RS annotation
    @Produces(MediaType.APPLICATION_XML) // KEEP: JAX-RS annotation
    public Response resolveLsk(String inputXml)  {
        log.info("Received POST request on /api/logical-seed-key/resolve");
        System.out.println("Entered Request handler"); // For basic testing

        // Check for null on the dependency (though constructor should prevent it)
        if (this.lskResolution == null) {
            log.error("Critical error: lskResolution field is null despite constructor injection!");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>Internal server configuration error.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        }

        if (inputXml == null || inputXml.trim().isEmpty()) {
            log.warn("Received empty or null XML input for resolution.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Request body requires XML content.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        }

        try {
            // Delegate processing to the service layer using the injected field
            String resolvedXml = this.lskResolution.processAndResolveXml(inputXml);
            log.info("LSK resolution successful for request.");
            log.info("");
            log.info("");
            log.info("Resolved XML:");
            log.info(resolvedXml);
            return Response.ok(resolvedXml).build();

        } catch (PlaceholderFormatException e) {
            log.warn("Placeholder format error during resolution: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Invalid placeholder format: " + e.getMessage() + "</error>") // Include msg
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (LskGenerationException e) {
            log.error("LSK Generation or DB error during resolution: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>LSK generation failed: " + e.getMessage() + "</error>") // Include msg
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (Exception e) { // Catch any other unexpected exceptions
            log.error("Unexpected internal server error during LSK resolution: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>An unexpected internal server error occurred.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        }
    }
}