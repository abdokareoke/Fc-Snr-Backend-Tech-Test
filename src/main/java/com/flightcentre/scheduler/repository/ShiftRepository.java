package com.flightcentre.scheduler.repository;

import com.flightcentre.scheduler.model.Shift;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Shift persistence.
 */
public interface ShiftRepository {

    Shift save(Shift shift);

    Optional<Shift> findById(String id);

    List<Shift> findAll();

    List<Shift> findAllById(List<String> ids);

    void deleteById(String id);

    boolean existsById(String id);
}
