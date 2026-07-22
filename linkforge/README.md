# LinkForge — Phase 1 Core

## Prerequisites
- Java 17+
- Maven 3.8+
- Internet access to Maven Central (for first build only)

## Run
```bash
mvn spring-boot:run
```
Service starts on `http://localhost:8080`.

## Try it
```bash
# Shorten a URL
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/some/very/long/path"}'

# Response:
# {"code":"1","shortUrl":"http://localhost:8080/1","longUrl":"https://www.example.com/some/very/long/path",...}

# Follow the redirect
curl -i http://localhost:8080/1

# Check analytics
curl http://localhost:8080/api/analytics/1
```

## Run tests
```bash
mvn test
```
Covers:
- `Base62EncoderTest` — encode/decode round-trip, uniqueness, invalid input
- `UrlShortenerIntegrationTest` — full shorten → redirect → analytics flow,
  validation rejection, 404 handling

## H2 console (dev only)
`http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:file:./data/urlshortener`, user `sa`, no password.

## Note on this sandbox
This project was built and code-reviewed in an environment without access to
Maven Central (network is allowlisted to a small set of domains that
doesn't include `repo.maven.apache.org`), so `mvn compile`/`mvn test` could
not be executed here. The code has been written and reviewed carefully for
correctness, but you should run `mvn test` on your own machine as the first
step before building on top of it.
