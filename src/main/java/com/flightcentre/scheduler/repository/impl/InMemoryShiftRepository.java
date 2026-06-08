package com.flightcentre.scheduler.repository.impl;

import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.repository.ShiftRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ShiftRepository.
 */
@Repository
public class InMemoryShiftRepository implements ShiftRepository {

    private final Map<String, Shift> store = new ConcurrentHashMap<>();

    @Override
    public Shift save(Shift shift) {
        store.put(shift.getId(), shift);
        return shift;
    }

    @Override
    public Optional<Shift> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Shift> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Shift> findAllById(List<String> ids) {
        return ids.stream()
                .map(store::get)
                .filter(shift -> shift != null)
                .collect(Collectors.toList());
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
