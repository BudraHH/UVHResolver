package com.budra.uvh.service;

import com.budra.uvh.dbConfig.ConnectionManager;
// Ensure correct package for repository
import com.budra.uvh.model.LskCounterRepository;
import com.budra.uvh.exception.PlaceholderFormatException;
// Assuming LskGenerationException might be thrown from repo or needed for future catches
import com.budra.uvh.exception.LskGenerationException;
import com.budra.uvh.utils.PlaceHolderInfo;
import com.budra.uvh.utils.XmlUtils;

// Removed jakarta.enterprise.context.RequestScoped
// Removed jakarta.inject.Inject
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// NO @RequestScoped annotation
public class LskResolution {
    private static final Logger log = LoggerFactory.getLogger(LskResolution.class);

    // Dependency field - made final, initialized by constructor
    private final LskCounterRepository counterRepository;

    // NO @Inject annotation

    // --- Constructor for Manual DI ---
    // This constructor must be called by whatever creates LskResolution
    // (e.g., the ManualDIProviderFactory)
    public LskResolution(LskCounterRepository counterRepository) {
        log.debug("LskResolution instance MANUALLY created via constructor.");
        if (counterRepository == null) {
            // Fail fast if the dependency wasn't provided during manual wiring
            throw new IllegalArgumentException("LskCounterRepository cannot be null for LskResolution");
        }
        this.counterRepository = counterRepository;
    }

    // Default no-arg constructor REMOVED - no longer needed for this manual approach


    public String processAndResolveXml(String inputXml) throws PlaceholderFormatException {
        log.info("Starting LSK resolution process for provided XML.");

        // Check dependency (though constructor should prevent null)
        if (this.counterRepository == null) {
            log.error("Critical error: counterRepository field is null despite constructor injection!");
            // Or throw a more specific internal error exception
            throw new LskGenerationException("Internal server configuration error: Repository unavailable.");
        }


        Map<String, PlaceHolderInfo> uniquePlaceholders = XmlUtils.findUniquePlaceholders(inputXml);

        if (uniquePlaceholders.isEmpty()) {
            log.info("No LSK placeholders found in the input XML. Returning original content.");
            return inputXml;
        }

        log.info("Found {} unique LSK placeholders to resolve.", uniquePlaceholders.size());

        Map<String, String> resolvedMappings = new HashMap<>();
        Connection connection = null;
        boolean transactionSuccess = false;

        try {
            connection = ConnectionManager.getConnection();
            connection.setAutoCommit(false);
            log.debug("Database transaction started for LSK generation.");

            for (Map.Entry<String, PlaceHolderInfo> entry : uniquePlaceholders.entrySet()) {
                String placeHolderKey = entry.getKey();
                PlaceHolderInfo info = entry.getValue();

                // Use the 'this.counterRepository' field
                long generatedValue = this.counterRepository.getAndReserveNextValueBlock(connection, info.getTableName(), info.getColumnName(), 1);
                // Use the buildResolvedLsk method from PlaceHolderInfo
                String resolvedLsk = info.buildResolvedLsk(generatedValue);

                resolvedMappings.put(placeHolderKey, resolvedLsk);
                log.debug("Mapped placeholder '{}' to resolved LSK '{}'", placeHolderKey, resolvedLsk);
            }

            connection.commit();
            transactionSuccess = true;
            log.debug("Database transaction committed successfully.");

        } catch (SQLException e) {
            log.error("SQL error during LSK generation transaction: {}", e.getMessage(), e);
            // Consider re-throwing as LskGenerationException if appropriate
            throw new LskGenerationException("Database error during LSK generation: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Catch potential validation errors from repo
            log.warn("Invalid argument during LSK generation: {}", e.getMessage());
            // Re-throw as LskGenerationException or handle as appropriate
            throw new LskGenerationException("Invalid data provided for LSK generation: " + e.getMessage(), e);
        }
        // Consider adding catch (LskGenerationException e) if getAndReserveNextValueBlock throws it directly
        // catch (LskGenerationException e) {
        //     log.error("LSK Generation error during resolution: {}", e.getMessage(), e);
        //     // Rollback happens in finally
        //     throw e; // Re-throw to be potentially handled by RequestHandler
        // }
        finally {
            // Finally block remains the same - crucial for connection handling
            if (connection != null) {
                try {
                    if (!transactionSuccess) {
                        log.warn("Rolling back transaction due to error during LSK resolution.");
                        connection.rollback();
                        log.info("Transaction rollback completed.");
                    }
                } catch (SQLException ex) {
                    log.error("!!! CRITICAL: Failed to rollback transaction !!!", ex);
                } finally {
                    try {
                        connection.setAutoCommit(true);
                        connection.close();
                        log.debug("Database connection returned to pool.");
                    } catch (SQLException e) {
                        log.error("Failed to close database connection/return to pool.", e);
                    }
                }
            }
        }
        log.debug("Replacing placeholders in XML content...");
        String resolvedXml = XmlUtils.replacePlaceholders(inputXml, resolvedMappings);

        log.info("LSK resolution service finished successfully.");
        return resolvedXml;
    }
}