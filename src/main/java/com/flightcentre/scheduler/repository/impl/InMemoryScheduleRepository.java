package com.flightcentre.scheduler.repository.impl;

import com.flightcentre.scheduler.model.Schedule;
import com.flightcentre.scheduler.repository.ScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ScheduleRepository.
 */
@Repository
public class InMemoryScheduleRepository implements ScheduleRepository {

    private final Map<String, Schedule> store = new ConcurrentHashMap<>();

    @Override
    public Schedule save(Schedule schedule) {
        store.put(schedule.getId(), schedule);
        return schedule;
    }

    @Override
    public Optional<Schedule> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Schedule> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }
}
