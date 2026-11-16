# CouponService

REST service for managing discount coupons.

## Prerequisites

- **Java 17 or newer** – needed to start Gradle. Set `JAVA_HOME` to it.
  The project itself uses Java 25. If you don't have it, Gradle downloads it automatically during the first build.
- **Docker with Docker Compose** (for example Docker Desktop) – must be running. Integration tests start a PostgreSQL
  database in Docker, and Docker Compose runs the whole application.

The commands below use `./gradlew`. On Windows, use `gradlew.bat` instead.

## Running tests

Unit tests (Docker is not needed):

```shell
./gradlew test
```

Integration tests (Docker must be running):

```shell
./gradlew integrationTest
```

All tests:

```shell
./gradlew check
```

Gradle skips tests if nothing has changed since the last run. To run them again anyway, add `--rerun`:

```shell
./gradlew integrationTest --rerun
```

Each module has its own test reports, for example:

- unit tests: `coupon-web/build/reports/tests/test/index.html`
- integration tests: `coupon-app/build/reports/tests/integrationTest/index.html`

## Building the application

Build the application and run all tests:

```shell
./gradlew build
```

Build the application without running tests:

```shell
./gradlew assemble
```

The application jar is created in `coupon-app/build/libs/coupon-app-0.0.1-SNAPSHOT.jar`.

## Running the application with Docker

Create the `.env` file with database passwords. The values from the example are fine for local use:

```shell
cp .env.example .env
```

Build and start the application and the PostgreSQL database:

```shell
docker compose up --build
```

The application connects to the database as the `empik` user, not as the default `postgres` superuser.

Check that the application is running:

```shell
curl http://localhost:8080/actuator/health
```

To stop everything, press `Ctrl+C` or run `docker compose down`. The database data stays in a Docker volume.
To delete the data as well, run `docker compose down -v`.

## API

### Create a coupon

```shell
curl -i -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "WIOSNA", "maxUses": 100, "country": "PL"}'
```

- `code` – letters, digits, `-` and `_`, up to 32 characters. Letter case does not matter: `wiosna` and `WIOSNA`
  are the same coupon. The code is saved in upper case.
- `maxUses` – how many times the coupon can be used, at least 1.
- `country` – two-letter ISO 3166-1 country code, for example `PL`.

The response is `201 Created` with the coupon in the body.

### Errors

Errors are returned in the Problem Details format (RFC 9457) with an extra `errorCode` field:

| Situation                            | HTTP status | `errorCode`             |
|--------------------------------------|-------------|-------------------------|
| Invalid request data                 | 400         | `VALIDATION_FAILED`     |
| Coupon with this code already exists | 409         | `COUPON_ALREADY_EXISTS` |
