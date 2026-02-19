# Transaction Type Service

Spring Boot REST API for Transaction Type Management, modernized from the CardDemo mainframe COBOL/CICS application.

## Overview

This microservice replaces the legacy CICS transactions (`CTTU`/`COTRTUPC` and `CTLI`/`COTRTLIC`) that manage transaction types in the CardDemo system. It provides RESTful CRUD endpoints backed by JPA/Hibernate with PostgreSQL (or H2 for local development).

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (for production) or H2 (for local development, enabled by default)

## Build

```bash
mvn clean package
```

## Run

```bash
mvn spring-boot:run
```

The application starts on port `8080` by default.

## API Endpoints

| Method | Endpoint                          | Description                  | Legacy Equivalent              |
|--------|-----------------------------------|------------------------------|--------------------------------|
| GET    | `/api/transaction-types`          | List all transaction types   | `COTRTLIC` cursor pagination   |
| GET    | `/api/transaction-types/{code}`   | Get transaction type by code | `COTRTUPC` read (lines 1469-1514) |
| POST   | `/api/transaction-types`          | Create a transaction type    | `COTRTUPC` insert (lines 1596-1623) |
| PUT    | `/api/transaction-types/{code}`   | Update a transaction type    | `COTRTUPC` update (lines 1531-1595) |
| DELETE | `/api/transaction-types/{code}`   | Delete a transaction type    | `COTRTUPC` delete (lines 1624-1666) |

## Request/Response Examples

### Create a Transaction Type

```bash
curl -X POST http://localhost:8080/api/transaction-types \
  -H "Content-Type: application/json" \
  -d '{"trType": "01", "trDescription": "Purchase"}'
```

**Response (201 Created):**
```json
{
  "trType": "01",
  "trDescription": "Purchase"
}
```

### Get All Transaction Types

```bash
curl http://localhost:8080/api/transaction-types
```

### Get a Transaction Type by Code

```bash
curl http://localhost:8080/api/transaction-types/01
```

### Update a Transaction Type

```bash
curl -X PUT http://localhost:8080/api/transaction-types/01 \
  -H "Content-Type: application/json" \
  -d '{"trDescription": "Purchase Transaction"}'
```

### Delete a Transaction Type

```bash
curl -X DELETE http://localhost:8080/api/transaction-types/01
```

**Response (204 No Content)** on success, or **409 Conflict** if child category records exist.

## Error Handling

| HTTP Status | Scenario                                         | Legacy SQLCODE |
|-------------|--------------------------------------------------|----------------|
| 404         | Transaction type not found                       | +100           |
| 409         | Delete blocked by child category records         | -532           |
| 409         | Record lock contention                           | -911           |
| 400         | Validation errors                                | N/A            |
| 409         | Duplicate transaction type code on create        | N/A            |

## Configuration

Edit `src/main/resources/application.properties` to configure:

- **Database**: Switch between H2 (default) and PostgreSQL/Aurora by uncommenting the appropriate datasource properties.
- **Server port**: `server.port` (default `8080`)
- **Logging**: Adjust `logging.level.*` properties as needed.

## Data Model

### TRANSACTION_TYPE
| Column         | Type         | Constraint  |
|----------------|--------------|-------------|
| tr_type        | CHAR(2)      | Primary Key |
| tr_description | VARCHAR(50)  |             |

### TRANSACTION_TYPE_CATEGORY
| Column            | Type         | Constraint                                |
|-------------------|--------------|-------------------------------------------|
| trc_type_code     | CHAR(2)      | Primary Key, FK to transaction_type.tr_type |
| trc_type_category | CHAR(4)      | Primary Key                               |
| trc_cat_data      | VARCHAR(50)  |                                           |

## H2 Console

When running with H2 (default profile), the H2 web console is available at:

```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:carddemo`
- Username: `sa`
- Password: *(empty)*
