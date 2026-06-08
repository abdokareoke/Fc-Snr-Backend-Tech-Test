package com.flightcentre.scheduler.repository;

import com.flightcentre.scheduler.model.Schedule;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Schedule persistence.
 */
public interface ScheduleRepository {

    Schedule save(Schedule schedule);

    Optional<Schedule> findById(String id);

    List<Schedule> findAll();

    void deleteById(String id);

    boolean existsById(String id);
}
