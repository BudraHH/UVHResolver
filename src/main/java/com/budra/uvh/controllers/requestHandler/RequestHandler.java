package com.budra.uvh.controllers.requestHandler;

// Correct import for your service class
import com.budra.uvh.controllers.service.LskResolution;

// Import standard Jakarta EE annotations for DI and scope
import jakarta.enterprise.context.RequestScoped; // To make this controller managed by CDI/HK2
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Import your custom exceptions
import com.budra.uvh.exception.LskGenerationException;
import com.budra.uvh.exception.PlaceholderFormatException;
import com.budra.uvh.controllers.service.LskResolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/logical-seed-key")
@RequestScoped // <<< ADD SCOPE: Make this controller managed by the DI container (HK2)
public class RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    @Inject // <<< CORRECT: Annotation is present, field is NOT initialized manually
    private LskResolution lskResolution;

    // Default constructor is fine for HK2
    public RequestHandler() {
        log.debug("RequestHandler instance created by DI container.");
    }

    @POST
    @Path("/resolve")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response resolveLsk(String inputXml){
        log.info("Received POST request on /api/logical-seed-key/resolve");
        System.out.println("Entered Request handler"); // Keep for basic testing if desired

        if (lskResolution == null) {
            // This check might still be useful during initial setup/debugging
            log.error("FATAL: LskResolution service was not injected! Check DI configuration (Annotations/Scanning/Binding in AppConfig).");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>Server Configuration Error: Service unavailable.</error>")
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
            String resolvedXml = lskResolution.processAndResolveXml(inputXml);
            log.info("LSK resolution successful for request.");
            return Response.ok(resolvedXml).build();

            // --- FIX: Uncomment specific exception handling ---
//        } catch (PlaceholderFormatException e) {
//            log.warn("Placeholder format error during resolution: {}", e.getMessage());
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity("<error>Invalid placeholder format: " + escapeXml(e.getMessage()) + "</error>")
//                    .type(MediaType.APPLICATION_XML)
//                    .build();
        } catch (LskGenerationException e) {
            log.error("LSK Generation or DB error during resolution: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>LSK generation failed: " + escapeXml(e.getMessage()) + "</error>")
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

    // Helper method to escape characters for XML output
    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "''")
                    .replace("'", "'");
    }
}