package com.flightcentre.scheduler.repository.impl;

import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.repository.EmployeeRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of EmployeeRepository.
 * Backed by a ConcurrentHashMap for thread-safe access.
 *
 * A secondary email index allows O(1) lookup by email
 * without scanning the entire employee map — used for
 * duplicate email validation on creation.
 */
@Repository
public class InMemoryEmployeeRepository implements EmployeeRepository {

    private final Map<String, Employee> store = new ConcurrentHashMap<>();
    // Secondary index: email -> id, for O(1) email uniqueness checks
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();

    @Override
    public Employee save(Employee employee) {
        store.put(employee.getId(), employee);
        emailIndex.put(employee.getEmail().toLowerCase(), employee.getId());
        return employee;
    }

    @Override
    public Optional<Employee> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        String id = emailIndex.get(email.toLowerCase());
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Employee> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        Employee employee = store.remove(id);
        if (employee != null) {
            emailIndex.remove(employee.getEmail().toLowerCase());
        }
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email.toLowerCase());
    }
}
