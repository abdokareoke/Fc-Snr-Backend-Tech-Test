package com.flightcentre.scheduler.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a named schedule covering a date range.
 * A schedule is the container that groups shifts together
 * for a given period (e.g. "Week of 2026-03-30").
 *
 * Shifts are stored as a list of IDs rather than embedded objects
 * to keep the domain model normalised — shifts are looked up
 * from the shift repository when needed.
 */
public class Schedule {

    private String id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> shiftIds;
    
    public Schedule() {
        this.shiftIds = new ArrayList<>();
    }

    public Schedule(String id, String name, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.shiftIds = new ArrayList<>();
    }

    public void addShiftId(String shiftId) {
        if (!shiftIds.contains(shiftId)) {
            shiftIds.add(shiftId);
        }
    }

    public void removeShiftId(String shiftId) {
        shiftIds.remove(shiftId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<String> getShiftIds() { return shiftIds; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setShiftIds(List<String> shiftIds) { this.shiftIds = shiftIds; }
}
