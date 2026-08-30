# Resource Booking System

A RESTful booking API built with Spring Boot, Spring Security, and JWT. Users browse resources and manage
their own reservations; administrators manage everything.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Security 7) |
| Persistence | Spring Data JPA / Hibernate 7 |
| Database | PostgreSQL 17 (Docker) — H2 in-memory for tests |
| Auth | JWT (JJWT 0.12.6), HS256, BCrypt password hashing |
| Docs | springdoc-openapi 3.1.0 (Swagger UI) |
| Build | Maven (wrapper included — no local Maven needed) |

## Prerequisites

- JDK 21+
- Docker (for PostgreSQL). Any PostgreSQL 14+ works if you'd rather run your own.

## Quick start

**1. Start the database**

```bash
docker compose up -d
```

This starts PostgreSQL 17 on **host port 5434**, not the default 5432. The container maps `5434:5432` so it
will not collide with a PostgreSQL server you may already have installed locally. If 5434 is taken on your
machine, change the host side of the mapping in `docker-compose.yml` and set `DB_URL` to match.

**2. Run the application**

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. On first start the schema is created and seed data is inserted.

**3. Open the API docs**

<http://localhost:8080/swagger-ui.html>

## Configuration

Every setting is read from an environment variable with a development fallback, so the app runs with no
configuration at all. Override any of them for a real deployment.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/bookingdb` | JDBC connection string |
| `DB_USER` | `booking` | Database user |
| `DB_PASS` | `booking` | Database password |
| `JWT_SECRET` | a development-only placeholder | HS256 signing key — **must be at least 32 bytes** |
| `JWT_EXPIRATION_MS` | `3600000` (1 hour) | Token lifetime |

Copy `.env.example` to `.env` for local overrides. `.env` is gitignored — never commit a real secret, and
never replace the placeholder in `application.properties` with a live value.

## Seed accounts

Created automatically on first start, with passwords hashed by BCrypt at runtime. The seeder is idempotent,
so restarting will not duplicate them.

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `alice` | `User@123` | USER |
| `bob` | `User@123` | USER |

Five resources are also seeded. One of them — *Drone - Survey Unit* — is deliberately marked unavailable so
the rejection path can be exercised.

## Authentication

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

Send the returned token on every other request:

```bash
curl http://localhost:8080/api/resources \
  -H "Authorization: Bearer <token>"
```

In Swagger UI, click **Authorize** and paste the token to call protected endpoints from the browser.

## Endpoints

### Authentication

| Method | Path | Access |
|---|---|---|
| POST | `/auth/login` | Public |

### Resources

| Method | Path | Access |
|---|---|---|
| GET | `/api/resources` | USER, ADMIN |
| GET | `/api/resources/{id}` | USER, ADMIN |
| POST | `/api/resources` | ADMIN |
| PUT | `/api/resources/{id}` | ADMIN |
| DELETE | `/api/resources/{id}` | ADMIN |

### Reservations

| Method | Path | Access |
|---|---|---|
| POST | `/api/reservations` | USER, ADMIN |
| GET | `/api/reservations` | Own only for USER; all for ADMIN |
| GET | `/api/reservations/{id}` | Owner or ADMIN |
| PUT | `/api/reservations/{id}` | Owner while PENDING, or ADMIN |
| DELETE | `/api/reservations/{id}` | Owner or ADMIN |

### Filtering, pagination, and sorting

```
GET /api/reservations?status=CONFIRMED&minPrice=100&maxPrice=500&page=0&size=10&sort=startTime,desc
```

| Parameter | Notes |
|---|---|
| `status` | `PENDING`, `CONFIRMED`, or `CANCELLED` |
| `minPrice` / `maxPrice` | Inclusive bounds |
| `page` / `size` | Defaults: page 0, size 10 |
| `sort` | `field,asc\|desc`. Allowed fields: `createdAt`, `endTime`, `id`, `price`, `startTime`, `status`. Anything else returns 400 rather than a server error. |

Responses are wrapped so the JSON contract stays stable:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

## Business rules

- **Reservation ownership comes from the JWT.** A `userId` in the request body is ignored, so a caller cannot
  book on another user's behalf.
- **New reservations are always `PENDING`.** A status supplied on create is ignored, and only an ADMIN can
  change status afterwards, so a user cannot approve their own booking.
- **Double booking is rejected** with 409. Two reservations for the same resource may not overlap in time;
  cancelled reservations free their slot.
- **Unavailable resources cannot be booked** — 409.
- **A resource with reservations cannot be deleted** — 409, with a suggestion to mark it unavailable instead.
  This prevents orphaned booking history.
- **A USER may edit their own reservation only while it is `PENDING`.** An ADMIN may edit any reservation.

## Error responses

Every failure returns the same shape, so clients parse one contract:

```json
{
  "timestamp": "2026-08-29T16:29:31",
  "status": 403,
  "error": "Forbidden",
  "message": "You may only access your own reservations",
  "path": "/api/reservations/7"
}
```

Validation failures add a `fieldErrors` object mapping each rejected field to its message.

| Status | When |
|---|---|
| 400 | Validation failed, unknown enum value, unsupported sort field, malformed JSON |
| 401 | Missing, malformed, or expired token; bad credentials |
| 403 | Authenticated but not permitted — wrong role, or another user's reservation |
| 404 | No such entity, or no such endpoint |
| 405 | Method not supported for that path |
| 409 | Overlapping booking, unavailable resource, resource still referenced, or reservation no longer PENDING |
| 500 | Unexpected error. Detail is logged server-side; the response body never includes a stack trace. |

## Tests

```bash
./mvnw test
```

51 tests run against in-memory H2, so no database or Docker is required.

| Suite | Covers |
|---|---|
| `JwtUtilTest` | Token generation, round-tripping, expiry, tampering, and forged signatures |
| `AuthControllerIT` | Login success and failure, token contract, 401 shape |
| `ResourceControllerIT` | USER read-only access, ADMIN writes, 403 enforcement |
| `ReservationOwnershipIT` | Identity from JWT, cross-user isolation, ADMIN visibility, status rules |
| `ReservationValidationIT` | Field validation, overlap, availability, filtering, pagination, sorting |

Security-critical assertions include a request that puts another user's id and `status: CONFIRMED` in the
body and asserts both are ignored, and a forged token signed with a different key that is rejected.

## Project structure

```
src/main/java/com/exelynt/booking/
├── config/       SecurityConfig, OpenApiConfig, PasswordEncoderConfig, DataSeeder
├── controller/   AuthController, ResourceController, ReservationController
├── dto/          request/ and response/ records — entities are never exposed
├── entity/       User, Resource, Reservation
├── enums/        Role, ReservationStatus
├── exception/    GlobalExceptionHandler and custom exceptions
├── repository/   Spring Data repositories and reservation specifications
├── security/     JwtUtil, JwtAuthFilter, CustomUserDetailsService, auth entry points
└── service/      AuthService, ResourceService, ReservationService
```

## Notes on the database

Schema is managed with `spring.jpa.hibernate.ddl-auto=update`, which suits an assignment where a single
command should produce a working database. A production system would use a migration tool such as Flyway.

Prices are stored as `NUMERIC(10,2)` and mapped to `BigDecimal` — never floating point.
