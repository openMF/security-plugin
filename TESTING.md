# Testing Guide — Selfservice Plugin

## Test Pyramid

```
Tier 4 │ *IntegrationTest.java  │ Testcontainers + @SpringBootTest + RestAssured │ mvn verify
Tier 3 │ Security slice tests   │ @SpringBootTest (mocked DB) + RestAssured      │ mvn verify
Tier 2 │ Spring wiring tests    │ @ContextConfiguration (no HTTP, no DB)         │ mvn test
Tier 1 │ *Test.java             │ Mockito unit tests                             │ mvn test
```

## Running Tests

```bash
# Tier 1 + 2: unit tests only (no Docker required, under 60s)
./mvnw clean test

# Tier 3 + 4: full pipeline including integration tests (Docker required)
./mvnw clean verify

# Run a single integration test class
./mvnw verify -Dit.test=SelfServiceAuthenticationIntegrationTest

# Skip spotless formatting check (useful during local development)
./mvnw verify -Dspotless.check.skip=true
```

## Coverage

JaCoCo measures both unit and integration test coverage and merges them before enforcing thresholds.

| Metric  | Measured Baseline | Enforced Floor | Goal       |
|---------|-------------------|----------------|------------|
| Line    | 54.60%            | 54%            | +5%/sprint |
| Method  | 56.74%            | 56%            | +5%/sprint |
| Branch  | 10.46%            | 10%            | +5%/sprint |

Coverage reports are written to:
- `target/site/jacoco/` — unit tests only
- `target/site/jacoco-merged/` — unit + integration (this is what the CI gate checks)

## Adding a New Integration Test

1. Create your test file with the `*IntegrationTest.java` suffix.
2. Extend `SelfServiceIntegrationTestBase`.
3. Inject `@LocalServerPort int port` and use `SelfServiceTestUtils.requestSpec(port)` for RestAssured calls.

```java
class MyFeatureIntegrationTest extends SelfServiceIntegrationTestBase {

    @Test
    void myEndpoint_withNoAuth_returns401() {
        given(SelfServiceTestUtils.requestSpec(port))
            .when().get("/api/v1/self/my-endpoint")
            .then().statusCode(401);
    }
}
```

## Naming Conventions

| Suffix             | Picked up by | Phase        |
|--------------------|--------------|--------------|
| `*Test.java`       | Surefire     | `test`       |
| `*IntegrationTest.java` | Failsafe | `verify`    |

## CI Structure

Three sequential GitHub Actions jobs:

1. **Unit Tests** — runs `mvn test`, uploads `jacoco.exec`
2. **Integration Tests** — runs `mvn verify -Dsurefire.skip=true`, uploads `jacoco-it.exec`
3. **Coverage Gate** — downloads both exec files, merges them, enforces thresholds

Both jobs run on every push and every pull request against `develop`.
