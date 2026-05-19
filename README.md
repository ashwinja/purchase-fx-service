# Purchase FX Service

Self-contained Java 21 REST API for storing U.S. dollar purchase transactions and retrieving them converted with Treasury Reporting Rates of Exchange.

The service is intentionally dependency-free: no database, servlet container, Maven, Gradle, Tomcat, Jetty, external framework, or IDE is required. It uses only the Java 21 JDK: the built-in HTTP server, Java HTTP client, file-backed JSON Lines persistence, and Java-only automated tests.

## Requirements covered

- Store a purchase transaction with a unique identifier.
- Validate description length is at most 50 characters.
- Validate transaction date as ISO-8601 `yyyy-MM-dd`.
- Validate positive purchase amount and round to the nearest cent.
- Persist purchases across restarts in `data/purchases.jsonl`.
- Retrieve a stored purchase converted to a Treasury-supported country/currency.
- Use the latest Treasury exchange rate `<= transactionDate` and `>= transactionDate - 6 months`.
- Return a conversion error if no qualifying rate is available.
- Round converted amount to two decimals.
- Include separated unit and functional tests.
- Include a dependency-free Java command-line client.

## Prerequisites

Required:

- Java 21+

Not required:

- Gradle
- Maven
- IDE
- Database
- Tomcat, Jetty, or servlet container

Verify Java:

```bash
java -version
javac -version
```

Both should show version 21 or later.


## Configuration files

This project uses Java property files for environment-based configuration:

| File | Purpose |
|---|---|
| `application.properties` | Shared defaults |
| `application-dev.properties` | Local development overrides |
| `application-test.properties` | Test defaults |
| `application-prod.properties` | Production defaults |

Important properties:

| Property | Description |
|---|---|
| `server.port` | Embedded HTTP server port |
| `purchase.storage.file` | Durable JSON Lines purchase store |
| `treasury.api.base-url` | Live Treasury FiscalData exchange-rate endpoint |

The application defaults to `prod`. Select an environment with `-Dapp.env=dev`, `-Dapp.env=test`, or `-Dapp.env=prod`. Individual values can be overridden with JVM system properties, for example `-Dserver.port=9090`.

## Run tests

The project separates fast unit tests from the HTTP functional test. The default test command runs both suites.

### Run all tests

Mac/Linux:

```bash
./scripts/test.sh
```

Windows PowerShell:

```powershell
.\scripts\test.ps1
```

Windows Command Prompt:

```bat
scripts\test.bat
```

### Run unit tests only

Mac/Linux:

```bash
./scripts/test-unit.sh
```

Windows PowerShell:

```powershell
.\scripts\test-unit.ps1
```

Windows Command Prompt:

```bat
scripts\test-unit.bat
```

### Run the functional test only

Mac/Linux:

```bash
./scripts/test-functional.sh
```

Windows PowerShell:

```powershell
.\scripts\test-functional.ps1
```

Windows Command Prompt:

```bat
scripts\test-functional.bat
```

If PowerShell blocks local scripts on your machine, run this once for the current PowerShell session only:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

The unit tests validate configuration loading, service behavior, and Treasury response parsing without requiring a live API call. The functional test starts the embedded HTTP server on a random local port and verifies API create/convert behavior end-to-end with a deterministic in-memory exchange-rate client.

In a larger production codebase, I would normally use JUnit and a mocking framework such as Mockito for test structure and mock verification. They are intentionally omitted here to keep the repository dependency-free and to avoid extra dependency downloads for reviewers. The Java client is not stubbed; it is a real HTTP client that calls the running service. The running application itself always uses the live Treasury FiscalData API for currency conversion. Configuration is loaded from Java property files under `src/main/resources`.

## Start the application on the built-in HTTP server. 

Mac/Linux:

```bash
./scripts/run.sh
```

Windows PowerShell:

```powershell
.\scripts\run.ps1
```

Windows Command Prompt:

```bat
scripts\run.bat
```

The service starts at:

```text
http://localhost:8080
```

Configuration:

The application uses environment-specific Java property files, not a `.env` file:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-test.properties
src/main/resources/application-prod.properties
```

By default, the service runs with `app.env=prod`. To select another property file, pass a JVM system property:

Mac/Linux:

```bash
java -Dapp.env=dev -cp build/classes com.example.purchasefx.Application
```

Windows PowerShell:

```powershell
java -Dapp.env=dev -cp build/classes com.example.purchasefx.Application
```

For production-style overrides, use JVM system properties. For example:

```bash
java -Dapp.env=prod \
  -Dserver.port=9090 \
  -Dpurchase.storage.file=data/prod-purchases.jsonl \
  -Dtreasury.api.base-url=https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange \
  -jar release/purchase-fx-service.jar
```

Currency conversion always calls the live Treasury FiscalData API. The API URL is configured through `treasury.api.base-url` in the property files so it is not hardcoded in the Java client code.

## Build and run executable JAR - another way to start the application using java command. Application is packaged as an executable jar file 

Mac/Linux:

```bash
./scripts/build-jar.sh
java -jar release/purchase-fx-service.jar
```

Windows PowerShell:

```powershell
.\scripts\build-jar.ps1
java -jar release\purchase-fx-service.jar
```

Windows Command Prompt:

```bat
scripts\build-jar.bat
java -jar release\purchase-fx-service.jar
```

## API - use curl from a separate terminal window to invoke the application API end points. Three operations

### Create purchase

```bash
curl -i -X POST http://localhost:8080/api/purchases \
  -H 'Content-Type: application/json' \
  -d '{"description":"Laptop bag","transactionDate":"2026-01-15","purchaseAmount":"120.129"}'
```

Example response:

```json
{
  "id": "d0d05b4f-44f8-4f0e-8e5f-c4f6d2d4968e",
  "description": "Laptop bag",
  "transactionDate": "2026-01-15",
  "purchaseAmountUsd": 120.13
}
```

### Get purchase

```bash
curl -i http://localhost:8080/api/purchases/{id}
```

### Convert purchase

Use the Treasury `country_currency_desc`, for example `Canada-Dollar`, `Euro Zone-Euro`, or `India-Rupee`.

```bash
curl -i 'http://localhost:8080/api/purchases/{id}/conversion?currency=Canada-Dollar'
```

Example response:

```json
{
  "id": "d0d05b4f-44f8-4f0e-8e5f-c4f6d2d4968e",
  "description": "Laptop bag",
  "transactionDate": "2026-01-15",
  "purchaseAmountUsd": 120.13,
  "targetCurrency": "Canada-Dollar",
  "exchangeRateUsed": 1.3690,
  "exchangeRateDate": "2025-12-31",
  "convertedAmount": 164.44
}
```

## Command-line Java client

A dependency-free Java command-line client is included so reviewers can exercise the API without an IDE, Postman, or curl.

Start the service in one terminal:

Mac/Linux:

```bash
./scripts/run.sh
```

Windows PowerShell:

```powershell
.\scripts\run.ps1
```

Use the client from another terminal.

Create a purchase:

Mac/Linux:

```bash
./scripts/client.sh create "Laptop bag" 2026-01-15 120.129
```

Windows PowerShell:

```powershell
.\scripts\client.ps1 create "Laptop bag" 2026-01-15 120.129
```

Windows Command Prompt:

```bat
scripts\client.bat create "Laptop bag" 2026-01-15 120.129
```

Get a purchase:

```bash
./scripts/client.sh get <purchase-id>
```

```powershell
.\scripts\client.ps1 get <purchase-id>
```

```bat
scripts\client.bat get <purchase-id>
```

Convert a purchase:

```bash
./scripts/client.sh convert <purchase-id> Canada-Dollar
```

```powershell
.\scripts\client.ps1 convert <purchase-id> Canada-Dollar
```

```bat
scripts\client.bat convert <purchase-id> Canada-Dollar
```

Target a different service URL:

Mac/Linux:

```bash
PURCHASE_FX_BASE_URL=http://localhost:8080 ./scripts/client.sh get <purchase-id>
./scripts/client.sh --base-url http://localhost:8080 convert <purchase-id> "Euro Zone-Euro"
```

Windows PowerShell:

```powershell
$env:PURCHASE_FX_BASE_URL = "http://localhost:8080"
.\scripts\client.ps1 get <purchase-id>
.\scripts\client.ps1 --base-url http://localhost:8080 convert <purchase-id> "Euro Zone-Euro"
```

Windows Command Prompt:

```bat
set PURCHASE_FX_BASE_URL=http://localhost:8080
scripts\client.bat get <purchase-id>
scripts\client.bat --base-url http://localhost:8080 convert <purchase-id> "Euro Zone-Euro"
```

Client commands:

```text
create  <description> <transaction-date-yyyy-MM-dd> <purchase-amount-usd>
get     <purchase-id>
convert <purchase-id> <treasury-country-currency-desc>
```

## Error model

All error responses use this shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Description must not exceed 50 characters"
  }
}
```

Status codes:

- `400` validation or malformed id/request.
- `404` purchase not found.
- `422` conversion unavailable/no qualifying Treasury rate.
- `500` unexpected server error.

## Treasury query

The client calls the FiscalData Treasury endpoint with:

```text
fields=country_currency_desc,exchange_rate,effective_date
filter=country_currency_desc:eq:{currency},effective_date:lte:{purchaseDate},effective_date:gte:{purchaseDateMinus6Months}
sort=-effective_date
page[size]=1
```

This selects the most recent rate on or before the purchase date and within six months.

## Architecture

- `api`: HTTP request/response handling and error mapping.
- `domain`: records and domain exceptions.
- `service`: validation, purchase storage use case, and conversion use case.
- `infra`: file-backed repository, JSON utility, and Treasury API client.
- `client`: command-line Java client that calls the service API.

## Production-hardening choices

- Durable file-backed persistence under the assignment constraint of no separately installed database.
- Dependency-free deployment to avoid hidden framework/container setup.
- Timeouts on outbound Treasury HTTP calls.
- Thread-pool backed HTTP server.
- Domain-level validation independent of API transport.
- Separated unit tests and HTTP functional test while keeping the repository dependency-free.

## Extension points

For a larger production deployment, I would add OpenAPI docs, structured logging, metrics, request IDs, exchange-rate caching, resilience policies for Treasury API outages, a standard JSON library, a robust Java framework like springboot to expose well defined REST end-points,  CI pipeline, container image publishing, and swap the repository interface to a managed database.
