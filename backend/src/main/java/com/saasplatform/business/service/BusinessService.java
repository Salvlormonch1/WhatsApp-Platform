package com.saasplatform.business.service;

import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessConfiguration;
import com.saasplatform.business.domain.BusinessHours;
import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Business management service.
 * All operations are tenant-scoped — businessId always comes from BusinessContext.
 */
@ApplicationScoped
public class BusinessService {

    private static final Logger LOG = Logger.getLogger(BusinessService.class);

    @Inject
    BusinessContext businessContext;

    /**
     * Get the current tenant's business profile.
     */
    public Business getCurrentBusiness() {
        UUID businessId = businessContext.getBusinessId();
        Business business = Business.findById(businessId);
        if (business == null) {
            throw new NotFoundException("Business");
        }
        return business;
    }

    /**
     * Update business profile fields.
     */
    @Transactional
    public Business updateBusiness(UpdateBusinessRequest request) {
        Business business = getCurrentBusiness();

        if (request.name() != null) business.name = request.name();
        if (request.description() != null) business.description = request.description();
        if (request.address() != null) business.address = request.address();
        if (request.phone() != null) business.phone = request.phone();
        if (request.email() != null) business.email = request.email();
        if (request.website() != null) business.website = request.website();
        if (request.timezone() != null) business.timezone = request.timezone();
        if (request.socialLinks() != null) business.socialLinks = request.socialLinks();

        LOG.debugf("Updated business %s", business.id);
        return business;
    }

    /**
     * Get the current business's AI/notification configuration.
     */
    public BusinessConfiguration getConfiguration() {
        UUID businessId = businessContext.getBusinessId();
        BusinessConfiguration config = BusinessConfiguration.findByBusinessId(businessId);
        if (config == null) {
            // Return defaults if not yet configured
            config = new BusinessConfiguration();
            config.businessId = businessId;
        }
        return config;
    }

    /**
     * Update business configuration.
     */
    @Transactional
    public BusinessConfiguration updateConfiguration(BusinessConfiguration updatedConfig) {
        UUID businessId = businessContext.getBusinessId();
        BusinessConfiguration config = BusinessConfiguration.findByBusinessId(businessId);

        if (config == null) {
            updatedConfig.businessId = businessId;
            updatedConfig.persist();
            return updatedConfig;
        }

        // Update fields
        if (updatedConfig.aiAssistantName != null) config.aiAssistantName = updatedConfig.aiAssistantName;
        if (updatedConfig.aiTone != null) config.aiTone = updatedConfig.aiTone;
        if (updatedConfig.aiCustomRules != null) config.aiCustomRules = updatedConfig.aiCustomRules;
        if (updatedConfig.aiFaqs != null) config.aiFaqs = updatedConfig.aiFaqs;
        if (updatedConfig.aiEscalationTriggers != null) config.aiEscalationTriggers = updatedConfig.aiEscalationTriggers;
        if (updatedConfig.notificationEmails != null) config.notificationEmails = updatedConfig.notificationEmails;
        config.weeklyReportEnabled = updatedConfig.weeklyReportEnabled;
        config.bookingLeadTimeMinutes = updatedConfig.bookingLeadTimeMinutes;
        config.bookingMaxDaysAhead = updatedConfig.bookingMaxDaysAhead;

        return config;
    }

    /**
     * Get business hours (sorted by day_of_week).
     */
    public List<BusinessHours> getHours() {
        return BusinessHours.findByBusiness(businessContext.getBusinessId());
    }

    /**
     * Update business hours (upsert all 7 days).
     */
    @Transactional
    public List<BusinessHours> updateHours(List<BusinessHours> hours) {
        UUID businessId = businessContext.getBusinessId();

        // Delete existing
        BusinessHours.delete("businessId = ?1", businessId);

        // Persist new schedule
        for (BusinessHours hour : hours) {
            hour.businessId = businessId;
            hour.id = null; // force insert
            hour.persist();
        }

        return BusinessHours.findByBusiness(businessId);
    }

    // Inner request records
    public record UpdateBusinessRequest(
            String name,
            String description,
            String address,
            String phone,
            String email,
            String website,
            String timezone,
            java.util.Map<String, String> socialLinks
    ) {}
}
