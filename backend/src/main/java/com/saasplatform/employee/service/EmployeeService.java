package com.saasplatform.employee.service;

import com.saasplatform.common.exception.NotFoundException;
import com.saasplatform.common.security.BusinessContext;
import com.saasplatform.employee.domain.Employee;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EmployeeService {

    @Inject
    BusinessContext businessContext;

    public List<Employee> listEmployees() {
        return Employee.findByBusiness(businessContext.getBusinessId());
    }

    public Employee getEmployee(UUID id) {
        Employee emp = Employee.findByIdAndBusiness(id, businessContext.getBusinessId());
        if (emp == null) throw new NotFoundException("Employee");
        return emp;
    }

    @Transactional
    public Employee createEmployee(EmployeeRequest request) {
        Employee emp = new Employee();
        emp.businessId = businessContext.getBusinessId();
        emp.name = request.name();
        emp.phone = request.phone();
        emp.email = request.email();
        emp.bio = request.bio();
        emp.persist();
        return emp;
    }

    @Transactional
    public Employee updateEmployee(UUID id, EmployeeRequest request) {
        Employee emp = getEmployee(id);
        if (request.name() != null) emp.name = request.name();
        if (request.phone() != null) emp.phone = request.phone();
        if (request.email() != null) emp.email = request.email();
        if (request.bio() != null) emp.bio = request.bio();
        return emp;
    }

    @Transactional
    public void deleteEmployee(UUID id) {
        Employee emp = getEmployee(id);
        emp.active = false;
    }

    public record EmployeeRequest(String name, String phone, String email, String bio) {}
}
