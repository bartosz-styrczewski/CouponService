# CouponService

REST service for managing discount coupons.

## Prerequisites

- **Java 17 or newer** – needed to start Gradle. Set `JAVA_HOME` to it.
  The project itself uses Java 25. If you don't have it, Gradle downloads it automatically during the first build.
- **Docker with Docker Compose** (for example Docker Desktop) – must be running for the tests and for running
  the application.

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Implementation notes

- **Architecture** – a layered application split into Gradle modules; the service and geolocation layers have
  separate API and implementation modules. Gradle enforces the layers at compile time, for example `coupon-web` can
  use only `coupon-service-api`. `coupon-app` puts all modules together.
- **Concurrency and scaling** – the application keeps no state except the cache, so it can run as many instances.
  Docker Compose starts two instances and an nginx load balancer in front of them, which listens on port 8080 and
  sends requests to the instances in turn. To start three instances, use `docker compose up --scale app=3`.
  The database guards the data:
  - the number of uses is increased by a single database update that works only while the coupon still has uses
    left. The database locks the coupon during this update, so parallel requests wait for each other and two of
    them can never take the last use. This is safe for many threads and many application instances,
  - the usage and the new number of uses are saved in one database transaction, so either both are saved or
    nothing is,
  - unique constraints block a duplicate coupon code and a second usage by the same user, also in parallel requests,
  - CHECK constraints are the last safety net.
- **`spring.jpa.open-in-view: false`** – with the default `true`, a request keeps its database connection until the
  response is sent, also while waiting for geolocation, so under load the connection pool can run out. With `false`
  the connection is held only while the database is used.
- **Client IP behind a load balancer or proxy** – the country is found from the client IP address. Behind a load
  balancer all requests come from the load balancer, so the application would see the load balancer's address for
  every client. With `server.forward-headers-strategy: native` the application can take the client address from
  the `X-Forwarded-For` header instead:
  - **load balancer or proxy** – it must add the client address to `X-Forwarded-For` and connect to the
    application from a trusted network. By default these are private networks (for example `10.0.0.0/8` or
    `192.168.0.0/16`); a load balancer with another address must be listed in
    `server.tomcat.remoteip.internal-proxies`,
  - **Docker Compose** – a `curl` to `localhost` does not go through the internet, so the application never sees
    your public address, only a private address of the Docker network, which has no country. That is why the API
    examples below set a public client address in the header by hand. The application accepts it, because the
    Docker network is private and therefore trusted,
  - **direct call from outside** – without a load balancer or proxy the client is not in a trusted network, so the
    header is ignored and the address the request really comes from is used. This way a client cannot pretend to
    be in another country.
- **Geolocation** – free providers [country.is](https://country.is) and [GeoJS](https://www.geojs.io), asked in the
  order set in `coupon.geolocation.providers` (one provider is enough). A provider is retried after connection
  errors, timeouts and 5xx; if it still fails, the next one is asked, and if all fail, the response is `503`.
  "Country unknown" is an answer, not a failure.
- **Cache** – countries are kept in memory in [Caffeine](https://github.com/ben-manes/caffeine) (up to 10,000 IP
  addresses for 24 hours). With `@Cacheable(sync = true)` parallel requests from the same IP ask the providers only
  once per instance. Failures are not cached.
- **Circuit breaker (possible next step)** – a provider that stopped responding is still asked with all retries,
  so with the current settings a request can wait over 20 seconds. A circuit breaker (for example
  [Resilience4j](https://resilience4j.readme.io)) would skip such a provider for a while.
- **Database** – PostgreSQL, schema managed only by Liquibase. The application connects as its own user `empik`
  (created by `docker/postgres/create-app-db.sh`), not as the superuser.
- **Configuration** – all settings are in `application.yml`, database credentials come from environment variables.
- **Tests** – unit tests cover the business logic with Mockito. Integration tests run the whole application with
  PostgreSQL (Testcontainers) and WireMock in place of the geolocation services, including a test with 50 parallel
  users.

## Running tests

```shell
./gradlew test
```

The test report is created in `coupon-app/build/reports/tests/test/aggregated-results/index.html`.

The same command runs on GitHub Actions for every push and pull request to `main`.

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

A coupon can only be used from its country, which is found from the client IP address. Locally the application
cannot see your public IP address, so the examples send a Polish IP address in the `X-Forwarded-For` header.

Linux and macOS:

```shell
curl -X POST http://localhost:8080/api/coupons/WIOSNA/usages \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 83.31.0.1" \
  -d '{"userId": "user-1"}'
```

Windows (PowerShell):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/coupons/WIOSNA/usages -ContentType "application/json" -Headers @{ "X-Forwarded-For" = "83.31.0.1" } -Body '{"userId": "user-1"}'
```

## Properties

Settings in `coupon-app/src/main/resources/application.yml` that make sense to change:

- `spring.cache.caffeine.spec` – cache size (`maximumSize`) and how long a country is kept (`expireAfterWrite`),
- `coupon.geolocation.providers` – providers in the order they are asked (`country-is`, `geojs`); one is enough,
- `coupon.geolocation.connect-timeout` – how long to wait for a connection to a provider,
- `coupon.geolocation.read-timeout` – how long to wait for an answer from a provider,
- `coupon.geolocation.retry.max-retries` – how many times a failed call to a provider is repeated,
- `coupon.geolocation.retry.delay` – the pause before each repeat,
- `coupon.geolocation.provider.<name>.url` – the address of each provider.
