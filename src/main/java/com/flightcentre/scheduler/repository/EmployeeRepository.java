package com.flightcentre.scheduler.repository;

import com.flightcentre.scheduler.model.Employee;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Employee persistence.
 * Services depend on this interface, not the in-memory implementation,
 * so swapping to a JPA implementation requires no changes to the service layer.
 */
public interface EmployeeRepository {

    Employee save(Employee employee);

    Optional<Employee> findById(String id);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAll();

    void deleteById(String id);

    boolean existsById(String id);

    boolean existsByEmail(String email);
}
