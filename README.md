# CouponService

REST service for managing discount coupons.

## Prerequisites

- **Java 17 or newer** – needed to start Gradle. Set `JAVA_HOME` to it.
  The project itself uses Java 25. If you don't have it, Gradle downloads it automatically during the first build.
- **Docker** – must be running. Integration tests start a PostgreSQL database in Docker.

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

Test reports:

- unit tests: `coupon-app/build/reports/tests/test/index.html`
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
