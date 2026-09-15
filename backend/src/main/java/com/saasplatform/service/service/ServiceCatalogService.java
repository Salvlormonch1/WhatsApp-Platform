package com.saasplatform.service.service;

import com.saasplatform.common.exception.BadRequestException;
import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.service.domain.Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ServiceCatalogService {

    @Inject
    BusinessContext businessContext;

    public List<Service> listServices() {
        return Service.findByBusiness(businessContext.getBusinessId());
    }

    public Service getService(UUID id) {
        Service service = Service.findByIdAndBusiness(id, businessContext.getBusinessId());
        if (service == null) throw new NotFoundException("Service");
        return service;
    }

    @Transactional
    public Service createService(ServiceRequest request) {
        Service service = new Service();
        service.businessId = businessContext.getBusinessId();
        service.name = request.name();
        service.description = request.description();
        service.price = request.price();
        service.durationMinutes = request.durationMinutes();
        service.persist();
        return service;
    }

    @Transactional
    public Service updateService(UUID id, ServiceRequest request) {
        Service service = getService(id);
        if (request.name() != null) service.name = request.name();
        if (request.description() != null) service.description = request.description();
        if (request.price() != null) service.price = request.price();
        if (request.durationMinutes() > 0) service.durationMinutes = request.durationMinutes();
        return service;
    }

    @Transactional
    public void deleteService(UUID id) {
        Service service = getService(id);
        service.active = false; // Soft delete
    }

    public record ServiceRequest(
            String name,
            String description,
            BigDecimal price,
            int durationMinutes
    ) {}
}
