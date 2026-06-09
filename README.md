# Shift Scheduling Engine

A production-ready REST API for workforce shift scheduling, built with Spring Boot 4.0.6 and Java 21.

## Assessment Context

1. Read [TECH_TEST.md](TECH_TEST.md) for the full problem specification
2. Read [SOLUTION.md](SOLUTION.md) for design decisions, trade-offs, and debrief answers

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+

### Run the Server

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080` with seed data pre-loaded (5 employees, 5 shifts, 1 schedule).

### Run the Tests

```bash
mvn test
```

### Build the JAR

```bash
mvn package
java -jar target/shift-scheduler-1.0.0.jar
```
