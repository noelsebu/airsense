# AirSense

A Spring Boot + Kotlin side project that mimics the [Airthings](https://www.airthings.com/) indoor air quality monitoring platform. Simulates real-world sensor data across multiple locations and devices, with a live dashboard, alert rules, and a REST API.

---

## Features

- **Sensor simulator** — scheduled job generating realistic IAQ data:
  - CO₂ follows weekday/weekend occupancy patterns
  - Radon rises when barometric pressure drops (weather-driven seepage model)
  - Temperature follows a sinusoidal day/night cycle
  - PM2.5 & VOC spike during kitchen meal times
  - Meeting rooms have bursty occupancy patterns
- **Thymeleaf dashboard** — dark-themed UI with Bootstrap 5
  - Location overview with air quality status tiles
  - Device detail with Chart.js sensor history graphs
  - Alert management (create rules, acknowledge alerts)
- **REST API** — `/api/v1/` endpoints for locations, devices, samples, and alerts
- **Flyway migrations** — versioned schema with demo seed data
- **H2 embedded database** — file-based, no external DB setup needed

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1 |
| Framework | Spring Boot 3.4 |
| Database | H2 (file-based) |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| Templates | Thymeleaf + htmx |
| Frontend | Bootstrap 5, Chart.js 4 |
| Build | Gradle (Kotlin DSL) |
| Java | 21 |

---

## Getting Started

### Prerequisites

- Java 21 ([Temurin](https://adoptium.net/temurin/releases/))

### Run

```bash
./gradlew bootRun
```

Open **http://localhost:8080**

---

## Demo Data

Three pre-seeded locations with 7 devices:

| Location | Devices |
|----------|---------|
| Stavanger HQ | Open Office (View Plus), Meeting Room A (Wave Plus), Basement Storage (Wave Radon) |
| Oslo Lab | Lab Floor (View Plus), Server Room (Space Pro) |
| Home Office | Living Room (Wave Mini), Bedroom (Wave Radon) |

Sensor data is generated every **10 seconds** by default (`airsense.simulator.interval-ms`).

---

## REST API

Base path: `/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/locations` | List all locations |
| GET | `/locations/{id}` | Location detail |
| GET | `/locations/{id}/devices` | Devices for a location |
| GET | `/locations/{id}/alerts` | Unacknowledged alerts |
| GET | `/devices/{id}` | Device detail |
| GET | `/devices/{id}/samples/latest` | Latest sample |
| GET | `/devices/{id}/samples/history?hours=24` | Sample history |
| GET | `/devices/{id}/samples/averages?hours=24` | Averages |
| GET | `/devices/{id}/alerts` | Alerts for device |
| GET | `/devices/{id}/alert-rules` | Alert rules for device |
| POST | `/devices/{id}/alert-rules` | Create alert rule |
| POST | `/alert-rules/{id}/toggle` | Enable/disable rule |
| DELETE | `/alert-rules/{id}` | Delete rule |
| GET | `/alerts/count` | Unacknowledged count |
| POST | `/alerts/{id}/acknowledge` | Acknowledge alert |

---

## Configuration

Key properties in `application.properties`:

```properties
airsense.simulator.enabled=true
airsense.simulator.interval-ms=10000
```

H2 console available at **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:file:./data/airsense`
- Username: `sa` / no password

---

## Project Structure

```
src/main/kotlin/com/airsense/
├── api/                  REST controllers
├── controller/           Thymeleaf MVC controllers
├── domain/               JPA entities + enums
├── dto/                  Data transfer objects + mappers
├── repository/           Spring Data repositories
├── service/              Business logic + event handling
└── simulator/            Sensor data simulator

src/main/resources/
├── db/migration/         Flyway SQL migrations
├── static/css/           Custom dark theme CSS
└── templates/            Thymeleaf HTML templates
```
