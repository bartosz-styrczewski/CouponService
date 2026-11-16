# CouponService

REST service for managing discount coupons.

## Prerequisites

- **Java 17 or newer** – needed to start Gradle. Set `JAVA_HOME` to it.
  The project itself uses Java 25. If you don't have it, Gradle downloads it automatically during the first build.
- **Docker with Docker Compose** (for example Docker Desktop) – must be running for the tests and for running
  the application.

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Running tests

```shell
./gradlew test
```

The test report is created in `coupon-app/build/reports/tests/test/aggregated-results/index.html`.

## Code coverage

```shell
./gradlew codeCoverageReport
```

The report is created in `coupon-app/build/reports/jacoco/testCodeCoverageReport/html/index.html`.

## Running the application in Docker Compose

Before the first run, copy `.env.example` to `.env`. It contains the local database passwords.

Linux and macOS:

```shell
cp .env.example .env
```

Windows (PowerShell):

```powershell
Copy-Item .env.example .env
```

Then build and start the application:

```shell
docker compose build
docker compose up
```

## API

### Create a coupon

`POST /api/coupons`

Linux and macOS:

```shell
curl -X POST http://localhost:8080/api/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "WIOSNA", "maxUses": 100, "country": "PL"}'
```

Windows (PowerShell):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/coupons -ContentType "application/json" -Body '{"code": "WIOSNA", "maxUses": 100, "country": "PL"}'
```

### Use a coupon

`POST /api/coupons/{code}/usages`

Linux and macOS:

```shell
curl -X POST http://localhost:8080/api/coupons/WIOSNA/usages \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-1"}'
```

Windows (PowerShell):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/coupons/WIOSNA/usages -ContentType "application/json" -Body '{"userId": "user-1"}'
```
