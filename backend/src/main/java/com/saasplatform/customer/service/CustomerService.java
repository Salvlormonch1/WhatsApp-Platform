package com.saasplatform.customer.service;

import com.saasplatform.common.dto.PageResponse;
import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.customer.domain.Customer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CustomerService {

    @Inject
    BusinessContext businessContext;

    public PageResponse<Customer> listCustomers(int page, int size, String search) {
        UUID businessId = businessContext.getBusinessId();

        io.quarkus.hibernate.orm.panache.PanacheQuery<Customer> query;
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            query = Customer.find(
                    "businessId = ?1 AND (lower(name) LIKE ?2 OR phone LIKE ?2)",
                    businessId, pattern
            );
        } else {
            query = Customer.find("businessId = ?1 ORDER BY lastContactAt DESC", businessId);
        }

        long total = query.count();
        List<Customer> customers = query.page(page, size).list();
        return PageResponse.of(customers, page, size, total);
    }

    public Customer getCustomer(UUID id) {
        Customer customer = Customer.findByIdAndBusiness(id, businessContext.getBusinessId());
        if (customer == null) throw new NotFoundException("Customer");
        return customer;
    }

    /** Upsert: find by phone or create new. Used by WhatsApp webhook. */
    @Transactional
    public Customer findOrCreateByPhone(String phone, UUID businessId, String name) {
        Customer existing = Customer.findByPhoneAndBusiness(phone, businessId);
        if (existing != null) {
            existing.lastContactAt = Instant.now();
            if (name != null && existing.name == null) {
                existing.name = name;
            }
            return existing;
        }

        Customer customer = new Customer();
        customer.businessId = businessId;
        customer.phone = phone;
        customer.name = name;
        customer.persist();
        return customer;
    }

    @Transactional
    public Customer updateCustomer(UUID id, CustomerUpdateRequest request) {
        Customer customer = getCustomer(id);
        if (request.name() != null) customer.name = request.name();
        if (request.email() != null) customer.email = request.email();
        if (request.notes() != null) customer.notes = request.notes();
        return customer;
    }

    public record CustomerUpdateRequest(String name, String email, String notes) {}
}
