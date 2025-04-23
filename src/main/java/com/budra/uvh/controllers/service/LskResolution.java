package com.budra.uvh.controllers.service;

// Correct imports based on your structure
import com.budra.uvh.dbConfig.ConnectionManager; // Assuming this is correct path
import com.budra.uvh.model.repository.LskCounterRepository;
import com.budra.uvh.exception.LskGenerationException;
import com.budra.uvh.exception.PlaceholderFormatException;
import com.budra.uvh.utils.PlaceHolderInfo; // Assuming correct path
import com.budra.uvh.utils.XmlUtils;      // Assuming correct path

// Import standard Jakarta EE annotations for DI and scope
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// --- FIX: Add Scope Annotation ---
@RequestScoped // Makes this service managed by HK2, typically one instance per request
public class LskResolution {
    private static final Logger log = LoggerFactory.getLogger(LskResolution.class);

    // --- FIX: Inject Repository ---
    @Inject
    private LskCounterRepository counterRepository;

    // --- FIX: Add Default Constructor (often needed for DI/proxies) ---
    public LskResolution() {
        log.debug("LskResolution service instance created by DI container.");
    }

    // --- FIX: Add throws clauses for specific exceptions ---
    public String processAndResolveXml(String inputXml) throws PlaceholderFormatException, LskGenerationException {
        log.info("Starting LSK resolution process for provided XML.");

        // --- FIX: Check for injected dependency ---
        if (counterRepository == null) {
            log.error("FATAL: LskCounterRepository was not injected into LskResolution! Check DI config.");
            throw new LskGenerationException("Internal server configuration error: Repository unavailable.");
        }

        // Use the correct method name from XmlUtils
        Map<String, PlaceHolderInfo> uniquePlaceholders = XmlUtils.findUniquePlaceholders(inputXml);

        if(uniquePlaceholders.isEmpty()){
            log.info("No LSK placeholders found in the input XML. Returning original content.");
            return inputXml;
        }

        log.info("Found {} unique LSK placeholders to resolve.", uniquePlaceholders.size());

        Map<String,String> resolvedMappings = new HashMap<>();

        Connection connection = null;
        boolean transactionSuccess = false;

        try {
            // Use the correct class name for ConnectionManager based on your import
            connection = ConnectionManager.getConnection();
            connection.setAutoCommit(false); // Start transaction
            log.debug("Database transaction started for LSK generation.");

            for(Map.Entry<String,PlaceHolderInfo> entry : uniquePlaceholders.entrySet()){
                String placeHolderKey = entry.getKey();
                PlaceHolderInfo info  = entry.getValue();

                // Use the correct method name from LskCounterRepository
                long generatedValue = counterRepository.getAndReserveNextValueBlock(connection, info.getTableName(), info.getColumnName(), 1);
                // Construct resolved LSK using PlaceholderInfo method
                String resolvedLsk = info.buildResolvedLsk(generatedValue);

                resolvedMappings.put(placeHolderKey, resolvedLsk);
                log.debug("Mapped placeholder '{}' to resolved LSK '{}'", placeHolderKey, resolvedLsk);
            }

            connection.commit(); // Commit transaction
            transactionSuccess = true;
            log.debug("Database transaction committed successfully.");

        } catch (SQLException e) {
            log.error("SQL error during LSK generation transaction: {}", e.getMessage(), e);
            // Rollback happens in finally block
            throw new LskGenerationException("Database error during LSK generation: " + e.getMessage(), e); // Re-throw specific exception
        } catch (IllegalArgumentException e) { // Catch validation errors from repo
            log.warn("Invalid argument during LSK generation: {}", e.getMessage());
            throw new LskGenerationException("Invalid data for LSK generation: " + e.getMessage(), e);
        }
        // Removed generic Exception catch here, specific ones are better.
        // Let runtime exceptions propagate if not caught specifically.
        finally {
            // --- Transaction Cleanup ---
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
                        connection.setAutoCommit(true); // Restore default
                        connection.close(); // Return connection to pool
                        log.debug("Database connection returned to pool.");
                    } catch (SQLException e) {
                        log.error("Failed to close database connection/return to pool.", e);
                    }
                }
            }
        }

        log.debug("Replacing placeholders in XML content...");
        // Use the correct method name from XmlUtils
        String resolvedXml = XmlUtils.replacePlaceholders(inputXml,resolvedMappings);

        log.info("LSK resolution service finished successfully.");
        return resolvedXml;
    }
}