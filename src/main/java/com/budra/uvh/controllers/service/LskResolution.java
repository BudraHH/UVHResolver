package com.budra.uvh.controllers.service;

import com.budra.uvh.dbConfig.ConnectionManager;
import com.budra.uvh.exception.PlaceholderFormatException;
import com.budra.uvh.model.repository.LskCounterRepository;
//import com.budra.uvh.controller.exception.LskGenerationException
//import com.budra.uvh.controller.exception.PlaceHolderFormatException
import com.budra.uvh.utils.PlaceHolderInfo;
import com.budra.uvh.utils.XmlUtils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RequestScoped
public class LskResolution {
    private static final Logger log = LoggerFactory.getLogger(LskResolution.class);

    @Inject
    private LskCounterRepository counterRepository;

    // Optional: Add a default constructor if needed (often good practice for DI)
    public LskResolution() {
        log.debug("LskResolution instance created by DI container.");
    }

    public String processAndResolveXml(String inputXml) throws PlaceholderFormatException {
        log.info("Starting LSK resolution process for provided XML.");

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
            connection = ConnectionManager.getConnection();
            connection.setAutoCommit(false);
            log.debug("Database transaction started for LSK generation.");

            for(Map.Entry<String,PlaceHolderInfo> entry : uniquePlaceholders.entrySet()){
                String placeHolderKey = entry.getKey();
                PlaceHolderInfo info  = entry.getValue();

                long generatedValue = counterRepository.getAndReserveNextValueBlock(connection, info.getTableName(), info.getColumnName(), 1);
                String resolvedLsk = info.getTableName() + ":" + info.getColumnName() + ":" + generatedValue;

                resolvedMappings.put(placeHolderKey, resolvedLsk);
                log.debug("Mapped placeholder '{}' to resolved LSK '{}'", placeHolderKey, resolvedLsk);
            }

            connection.commit();
            transactionSuccess = true;
            log.debug("Database transaction committed successfully.");

        } catch (SQLException e) {
            log.error("SQL error during LSK generation transaction: {}", e.getMessage(), e);
//            throw exception
//        } catch (LskGenerationException | PlaceholderFormatException e) { // Catch specific exceptions
//            log.error("Error during LSK resolution: {}", e.getMessage());
//            // Rollback needed here too
//            throw e; // Re-throw to be handled by the controller
//        }
//        catch (Exception e) { // Catch any other unexpected runtime errors
//            log.error("Unexpected error during LSK resolution service: {}", e.getMessage(), e);
//            // Rollback needed
//            throw new LskGenerationException("An unexpected error occurred: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    if (!transactionSuccess) {
                        log.warn("Rolling back transaction due to error during LSK resolution.");
                        connection.rollback();
                        log.info("Transaction rollback completed.");
                    }
                } catch (SQLException e) {
                    log.error("!!! CRITICAL: Failed to rollback transaction !!!", e);
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
        String resolvedXml = XmlUtils.replacePlaceholders(inputXml,resolvedMappings);

        log.info("LSK resolution service finished successfully.");
        return resolvedXml;
    }
}
