package com.budra.uvh.controllers.requestHandler;

// Correct import for your service class (assuming it's now LskResolutionService)
import com.budra.uvh.controllers.service.LskResolution; // USE THE CORRECT CLASS NAME

// Import standard Jakarta EE annotations for DI and scope
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Import your custom exceptions
import com.budra.uvh.exception.LskGenerationException;
import com.budra.uvh.exception.PlaceholderFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/logical-seed-key")
@RequestScoped // <<< IMPORTANT: Make the controller itself injectable/managed
public class RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    @Inject
    private LskResolution lskResolution; // <<< Use the correctly annotated service class

    // HK2 uses the default constructor
    public RequestHandler() {
        log.debug("RequestHandler instance created by DI container.");
    }

    @POST
    @Path("/resolve")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response resolveLsk(String inputXml) throws PlaceholderFormatException {
        log.info("Received POST request on /api/logical-seed-key/resolve");
        System.out.println("Entered Request handler"); // For basic testing

        // The lskResolution == null check is less useful here,
        // as the UnsatisfiedDependencyException would have already occurred
        // during request processing if injection failed.

        if (inputXml == null || inputXml.trim().isEmpty()) {
            log.warn("Received empty or null XML input for resolution.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Request body requires XML content.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        }

        try {
            // Delegate processing to the service layer
            String resolvedXml = lskResolution.processAndResolveXml(inputXml);
            log.info("LSK resolution successful for request.");
            return Response.ok(resolvedXml).build();

            // --- Specific exception handling is better ---
        } catch (PlaceholderFormatException e) {
            log.warn("Placeholder format error during resolution: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("<error>Invalid placeholder format.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (LskGenerationException e) {
            log.error("LSK Generation or DB error during resolution: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>LSK generation failed.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        } catch (Exception e) { // Catch any other unexpected exceptions
            System.out.println("Hari hara budra Error here!!!!!!!!!!!!!!!!!!!!!!!!");
            log.error("Unexpected internal server error during LSK resolution: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("<error>An unexpected internal server error occurred.</error>")
                    .type(MediaType.APPLICATION_XML)
                    .build();
        }
    }


}