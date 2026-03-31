# KWATERA

**KWATERA** is a web-based system for managing holiday accommodation bookings, availability, utility settlements, payments, reporting, and administration.

The project name expands to Polish:

**K**ompleksowy **W**ebowy **A**systent **T**erminarza, **E**nergii, **R**ezerwacji i **A**dministracji

## Project Overview

KWATERA is being developed as a semester project focused on building a realistic accommodation management platform rather than a simple CRUD application. The system is intended to support the full reservation lifecycle, including guest booking flows, availability handling, deposit and payment tracking, final settlement of stays, and administrative supervision.

In addition to standard booking features, the project includes AI-assisted modules such as OCR-based meter reading from uploaded images, pricing or occupancy support based on historical reservation data, and weather-informed reservation analysis.

## Planned Scope

The target system is expected to include:

- guest-facing booking and availability search
- administration and reception workflows
- reservation status management
- deposits, payments, balances, and billing
- utility settlement based on meter readings
- notifications and reminders
- reporting and operational dashboards
- AI OCR for meter reading support
- predictive analytics for pricing or demand
- weather-based scoring support

## Target Architecture

The project is planned as a microservice-based solution with:

- Java Spring Boot as the main backend technology
- a separate Python AI OCR service
- PostgreSQL as the primary database
- Spring Cloud Eureka for service discovery
- Spring Cloud Config Server for centralized configuration
- Spring Authorization Server for authentication and authorization
- Docker and Docker Compose for local development and deployment
- OpenAPI-based API documentation

## Roles

The main user roles considered in the project are:

- Administrator
- Reception
- Guest

## Repository Workflow

The project is intended to be developed with a team workflow based on:

- feature branches
- pull requests
- code review
- CODEOWNERS
- GitHub Issues and Kanban-based task tracking

## Local run

Run the empty runtime environment with Docker Compose:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

### Useful URLs after startup:

Config Server health: http://localhost:8888/actuator/health
Eureka dashboard: http://localhost:8761
Reservation service ping: http://localhost:8080/api/ping
Reservation service health: http://localhost:8080/actuator/health