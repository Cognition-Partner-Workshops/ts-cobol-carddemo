 g# CardDemo Spring Boot Application

Spring Boot migration of the AWS CardDemo mainframe COBOL/CICS application. This project provides a modern Java-based implementation of the credit card management system originally running on a mainframe environment.

## Prerequisites

- Java 17 or later
- Maven 3.8+ (or use the included Maven wrapper)

## Project Structure

```
spring-boot/
├── src/main/java/com/aws/carddemo/
│   ├── CardDemoApplication.java        # Main entry point
│   ├── account/                        # Account management domain
│   ├── authorization/                  # Transaction authorization logic
│   ├── billing/                        # Billing and statement generation
│   ├── card/                           # Credit card management domain
│   ├── config/                         # Application configuration (security, etc.)
│   ├── controller/                     # REST API controllers
│   ├── customer/                       # Customer management domain
│   ├── report/                         # Reporting domain
│   ├── transaction/                    # Transaction processing domain
│   └── user/                           # Application user management
├── src/main/resources/
│   ├── application.yml                 # Application configuration with profiles
│   └── db/migration/                   # Flyway database migrations
└── src/test/java/com/aws/carddemo/    # Test sources
```

## Running the Application

### Development Mode (H2 in-memory database)

```bash
./mvnw spring-boot:run
```

The application starts on port 8080 with the `dev` profile active by default. An H2 console is available at http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:carddemo`).

### Running with a Specific Profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

The `prod` profile requires the following environment variables:

| Variable            | Description                        |
|---------------------|------------------------------------|
| `DATABASE_URL`      | PostgreSQL JDBC connection URL     |
| `DATABASE_USERNAME` | Database username                  |
| `DATABASE_PASSWORD` | Database password                  |

### Verifying the Application

Once running, check the health endpoint:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "application": "CardDemo",
  "timestamp": "2026-01-01T00:00:00Z"
}
```

## Running Tests

```bash
./mvnw test
```

## Building a JAR

```bash
./mvnw clean package
java -jar target/carddemo-0.0.1-SNAPSHOT.jar
```

## Profiles

| Profile | Database   | Use Case                           |
|---------|------------|------------------------------------|
| `dev`   | H2 memory  | Local development, H2 console on   |
| `test`  | H2 memory  | Automated testing                  |
| `prod`  | PostgreSQL | Production deployment              |

## Database Migrations

Flyway manages schema migrations in `src/main/resources/db/migration/`. Migrations follow the naming convention `V{version}__{description}.sql` and are applied automatically on startup.
