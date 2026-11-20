# Java skeleton

Requires JDK 21 and Docker (running) for PostgreSQL 15 Testcontainers tests.

- `common`: shared, framework-independent types.
- `codec`: plain Java fixed-length record codec and unit tests.
- `batch`: Spring Boot / Spring Batch 5 with JDBC and PostgreSQL.
- `online`: Spring Boot REST app with JDBC, PostgreSQL and `/actuator/health`.

Build and test all modules from this directory:

```sh
./mvnw -B verify
```

To run either application, configure `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
No business jobs or database migrations are included yet.
