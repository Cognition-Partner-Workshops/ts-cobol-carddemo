# CardDemo Transaction Processing Module - Modernized

Cloud-native implementation of the legacy CICS/COBOL Transaction Processing Module, built with Java 21, Spring Boot 3, React, and PostgreSQL.

## Architecture

| Component | Technology | Port |
|-----------|-----------|------|
| Backend   | Java 21 / Spring Boot 3 / Spring Data JPA | 8080 |
| Frontend  | React 18 / TypeScript / Vite | 3000 |
| Database  | PostgreSQL 15 | 5432 |

## Business Rules Coverage (30/30)

### CT00 - Transaction List (BR-LT-01 to BR-LT-08)
- Fixed page size of 10 transactions
- Numeric Transaction ID filter validation
- Forward/backward pagination with boundary detection
- Selection-based navigation to detail view

### CT01 - Transaction View (BR-VT-01 to BR-VT-05)
- Transaction ID lookup with existence validation
- Read-only display of all 13 detail fields
- Cross-reference resolution for Account ID display

### CT02 - Transaction Add (BR-AT-01 to BR-AT-14)
- 6-phase validation chain executed in order:
  1. Key Field Validation (Account/Card required, numeric, exists)
  2. Mandatory Field Checks (11 fields)
  3. Numeric Type Checks (Type Code, Category Code)
  4. Amount Format Validation (-99999999.99)
  5. Date Validation (format + calendar validity)
  6. Merchant ID Numeric Check
- Y/N confirmation gate
- Thread-safe Transaction ID auto-generation via PostgreSQL sequence
- Duplicate ID rejection
- Bidirectional cross-reference resolution (Account ID <-> Card Number)

### Common Functions (BR-CF-01 to BR-CF-03)
- PF3: Back to Menu
- PF4: Clear screen/form
- PF5: Back to List (CT01) / Copy Last Transaction (CT02)

## Quick Start with Docker Compose

```bash
cd modernization-package
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- PostgreSQL: localhost:5432

## Manual Setup

### Prerequisites
- Java 21 (JDK)
- Maven 3.9+
- Node.js 20+
- PostgreSQL 15+

### Database Setup

```bash
# Create database and user
psql -U postgres -c "CREATE USER carddemo WITH PASSWORD 'carddemo';"
psql -U postgres -c "CREATE DATABASE carddemo OWNER carddemo;"
```

Flyway migrations run automatically on backend startup.

### Backend

```bash
cd modernization-package/backend
mvn clean package -DskipTests
java -jar target/transaction-processing-1.0.0.jar
```

The backend starts on port 8080. Configuration can be overridden via environment variables:
- `SPRING_DATASOURCE_URL` (default: jdbc:postgresql://localhost:5432/carddemo)
- `SPRING_DATASOURCE_USERNAME` (default: carddemo)
- `SPRING_DATASOURCE_PASSWORD` (default: carddemo)

### Frontend

```bash
cd modernization-package/frontend
npm install
npm run dev
```

The frontend starts on port 3000 with API proxy to localhost:8080.

## API Endpoints

| Method | Endpoint | Description | Screen |
|--------|----------|-------------|--------|
| GET | /api/v1/transactions | List transactions (paginated) | CT00 |
| GET | /api/v1/transactions/{id} | View transaction detail | CT01 |
| GET | /api/v1/transactions/latest | Get latest transaction (Copy Last) | CT02 |
| POST | /api/v1/transactions | Add transaction (6-phase validation) | CT02 |
| GET | /api/v1/cross-references/resolve | Resolve Account ID <-> Card Number | CT02 |

## Project Structure

```
modernization-package/
├── backend/
│   ├── src/main/java/com/carddemo/transaction/
│   │   ├── config/          # WebConfig (CORS), JacksonConfig
│   │   ├── controller/      # TransactionController, CrossReferenceController
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── entity/          # JPA entities (Transaction, Account, Card, etc.)
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/      # Spring Data JPA repositories
│   │   └── service/         # Business logic (6-phase validation, cross-ref)
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/    # Flyway V1-V7 scripts
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── pages/           # TransactionList, TransactionView, TransactionAdd
│   │   ├── services/        # API client
│   │   ├── App.tsx          # Router configuration
│   │   └── index.css        # Terminal-themed styles
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml
└── README.md
```
