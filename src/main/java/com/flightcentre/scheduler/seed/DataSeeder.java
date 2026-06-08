package com.flightcentre.scheduler.seed;

import com.flightcentre.scheduler.model.Employee;
import com.flightcentre.scheduler.model.Schedule;
import com.flightcentre.scheduler.model.Shift;
import com.flightcentre.scheduler.model.enums.ContractType;
import com.flightcentre.scheduler.model.enums.Skill;
import com.flightcentre.scheduler.model.valueobjects.AvailabilityWindow;
import com.flightcentre.scheduler.repository.EmployeeRepository;
import com.flightcentre.scheduler.repository.ScheduleRepository;
import com.flightcentre.scheduler.repository.ShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Loads seed data on application startup as required by the spec.
 * This allows the API to be tested immediately without manual setup.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final ScheduleRepository scheduleRepository;

    public DataSeeder(EmployeeRepository employeeRepository,
                      ShiftRepository shiftRepository,
                      ScheduleRepository scheduleRepository) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedEmployees();
        seedShifts();
        seedSchedule();
        log.info("Seed data loaded: 5 employees, 5 shifts, 1 schedule");
    }

    private void seedEmployees() {
        // Standard Mon-Fri 06:00-22:00 availability for all seed employees
        List<AvailabilityWindow> monToFri = List.of(
                new AvailabilityWindow(DayOfWeek.MONDAY,    LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.TUESDAY,   LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.WEDNESDAY, LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.THURSDAY,  LocalTime.of(6, 0), LocalTime.of(22, 0)),
                new AvailabilityWindow(DayOfWeek.FRIDAY,    LocalTime.of(6, 0), LocalTime.of(22, 0))
        );

        employeeRepository.save(new Employee(
                "emp-01", "Alice Johnson", "alice.johnson@example.com",
                ContractType.FULL_TIME, 40.0,
                List.of(Skill.SUPERVISOR, Skill.FIRST_AID),
                monToFri));

        employeeRepository.save(new Employee(
                "emp-02", "Bob Smith", "bob.smith@example.com",
                ContractType.FULL_TIME, 40.0,
                List.of(Skill.FORKLIFT_CERTIFIED),
                monToFri));

        employeeRepository.save(new Employee(
                "emp-03", "Carol White", "carol.white@example.com",
                ContractType.PART_TIME, 20.0,
                List.of(Skill.FIRST_AID),
                monToFri));

        employeeRepository.save(new Employee(
                "emp-04", "David Brown", "david.brown@example.com",
                ContractType.FULL_TIME, 40.0,
                List.of(Skill.FORKLIFT_CERTIFIED, Skill.SUPERVISOR),
                monToFri));

        employeeRepository.save(new Employee(
                "emp-05", "Eva Martinez", "eva.martinez@example.com",
                ContractType.PART_TIME, 20.0,
                List.of(Skill.FIRST_AID, Skill.FORKLIFT_CERTIFIED),
                monToFri));
    }

    private void seedShifts() {
        // Week of 2026-03-30 (Monday start)
        LocalDate monday = LocalDate.of(2026, 3, 30);
        LocalDate tuesday = monday.plusDays(1);
        LocalDate wednesday = monday.plusDays(2);

        shiftRepository.save(new Shift(
                "shf-01", "Warehouse A",
                LocalDateTime.of(monday, LocalTime.of(6, 0)),
                LocalDateTime.of(monday, LocalTime.of(14, 0)),
                2, List.of(Skill.FORKLIFT_CERTIFIED)));

        shiftRepository.save(new Shift(
                "shf-02", "Warehouse A",
                LocalDateTime.of(monday, LocalTime.of(14, 0)),
                LocalDateTime.of(monday, LocalTime.of(22, 0)),
                2, List.of(Skill.FORKLIFT_CERTIFIED)));

        shiftRepository.save(new Shift(
                "shf-03", "Office",
                LocalDateTime.of(monday, LocalTime.of(8, 0)),
                LocalDateTime.of(monday, LocalTime.of(16, 0)),
                1, List.of(Skill.SUPERVISOR)));

        shiftRepository.save(new Shift(
                "shf-04", "Warehouse A",
                LocalDateTime.of(tuesday, LocalTime.of(6, 0)),
                LocalDateTime.of(tuesday, LocalTime.of(14, 0)),
                1, List.of(Skill.FORKLIFT_CERTIFIED)));

        shiftRepository.save(new Shift(
                "shf-05", "Warehouse B",
                LocalDateTime.of(tuesday, LocalTime.of(22, 0)),
                LocalDateTime.of(wednesday, LocalTime.of(6, 0)),
                2, List.of(Skill.FIRST_AID)));
    }

    private void seedSchedule() {
        Schedule schedule = new Schedule(
                "sch-01", "Week of 2026-03-30",
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 5));

        schedule.addShiftId("shf-01");
        schedule.addShiftId("shf-02");
        schedule.addShiftId("shf-03");
        schedule.addShiftId("shf-04");
        schedule.addShiftId("shf-05");

        scheduleRepository.save(schedule);
    }
}
