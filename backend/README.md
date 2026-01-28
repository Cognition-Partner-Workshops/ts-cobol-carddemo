# CardDemo Backend - Java Spring Boot Microservices

This is the modernized backend for the CardDemo credit card management application, migrated from COBOL/CICS mainframe to Java Spring Boot microservices architecture.

## Architecture Overview

The application consists of the following microservices:

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | API Gateway for routing and authentication |
| auth-service | 8081 | Authentication and authorization with JWT |
| customer-service | 8082 | Customer management |
| account-service | 8083 | Account management |
| card-service | 8084 | Credit card management |
| transaction-service | 8085 | Transaction processing |
| payment-service | 8086 | Bill payment processing |
| reporting-service | 8087 | Reporting and analytics |
| batch-service | 8088 | Batch processing with Spring Batch |

## Technology Stack

- Java 21
- Spring Boot 3.2.2
- Spring Cloud 2023.0.0
- Spring Data JPA
- Spring Security with JWT
- Spring Batch
- PostgreSQL (production) / H2 (development)
- Maven

## Prerequisites

- JDK 21 or higher
- Maven 3.8+
- PostgreSQL 15+ (for production)
- Docker & Docker Compose (optional)

## Building the Project

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests
```

## Running Locally

### Option 1: Run Individual Services

```bash
# Start each service in separate terminals
cd auth-service && mvn spring-boot:run
cd customer-service && mvn spring-boot:run
cd account-service && mvn spring-boot:run
cd card-service && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd reporting-service && mvn spring-boot:run
cd batch-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### Option 2: Using Docker Compose

```bash
docker-compose up -d
```

## API Documentation

Each service exposes Swagger UI for API documentation:

- Auth Service: http://localhost:8081/swagger-ui.html
- Customer Service: http://localhost:8082/swagger-ui.html
- Account Service: http://localhost:8083/swagger-ui.html
- Card Service: http://localhost:8084/swagger-ui.html
- Transaction Service: http://localhost:8085/swagger-ui.html
- Payment Service: http://localhost:8086/swagger-ui.html
- Reporting Service: http://localhost:8087/swagger-ui.html
- Batch Service: http://localhost:8088/swagger-ui.html

## API Endpoints

### Authentication
- POST `/api/v1/auth/login` - User login
- POST `/api/v1/auth/register` - User registration
- POST `/api/v1/auth/refresh` - Refresh token
- POST `/api/v1/auth/change-password` - Change password
- GET `/api/v1/auth/me` - Get current user

### Customers
- GET `/api/v1/customers` - List all customers
- GET `/api/v1/customers/{id}` - Get customer by ID
- POST `/api/v1/customers` - Create customer
- PUT `/api/v1/customers/{id}` - Update customer
- DELETE `/api/v1/customers/{id}` - Delete customer

### Accounts
- GET `/api/v1/accounts` - List all accounts
- GET `/api/v1/accounts/{id}` - Get account by ID
- POST `/api/v1/accounts` - Create account
- PUT `/api/v1/accounts/{id}` - Update account
- POST `/api/v1/accounts/{id}/activate` - Activate account
- POST `/api/v1/accounts/{id}/deactivate` - Deactivate account

### Cards
- GET `/api/v1/cards` - List all cards
- GET `/api/v1/cards/{cardNumber}` - Get card by number
- POST `/api/v1/cards` - Create card
- PUT `/api/v1/cards/{cardNumber}` - Update card
- POST `/api/v1/cards/{cardNumber}/activate` - Activate card
- POST `/api/v1/cards/{cardNumber}/deactivate` - Deactivate card

### Transactions
- GET `/api/v1/transactions` - List all transactions
- GET `/api/v1/transactions/{id}` - Get transaction by ID
- POST `/api/v1/transactions` - Create transaction
- GET `/api/v1/transactions/card/{cardNumber}` - Get transactions by card

### Payments
- GET `/api/v1/payments` - List all payments
- GET `/api/v1/payments/{id}` - Get payment by ID
- POST `/api/v1/payments` - Create payment
- POST `/api/v1/payments/{id}/process` - Process payment
- POST `/api/v1/payments/{id}/cancel` - Cancel payment

### Reports
- GET `/api/v1/reports/dashboard` - Dashboard summary
- GET `/api/v1/reports/account/{id}/statement` - Account statement
- GET `/api/v1/reports/transactions` - Transaction report

### Batch Jobs
- POST `/api/v1/batch/interest-calculation` - Run interest calculation
- POST `/api/v1/batch/transaction-posting` - Run transaction posting

## Configuration

### Environment Variables (Production)

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=carddemo
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=your-256-bit-secret-key
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
```

## Project Structure

```
carddemo-backend/
├── common/                 # Shared entities, DTOs, exceptions
├── auth-service/          # Authentication service
├── customer-service/      # Customer management
├── account-service/       # Account management
├── card-service/          # Card management
├── transaction-service/   # Transaction processing
├── payment-service/       # Payment processing
├── reporting-service/     # Reporting and analytics
├── batch-service/         # Batch processing
├── api-gateway/           # API Gateway
├── docker-compose.yml     # Docker Compose configuration
└── pom.xml               # Parent POM
```

## Migration from COBOL/CICS

This application is a modernized version of the AWS CardDemo mainframe application. Key mappings:

| COBOL Program | Java Service | Description |
|---------------|--------------|-------------|
| COSGN00C | auth-service | Sign-on/authentication |
| COACTVWC | account-service | Account view |
| COACTUPC | account-service | Account update |
| COCRDSLC | card-service | Card list |
| COCRDUPC | card-service | Card update |
| COTRN00C | transaction-service | Transaction list |
| COTRN01C | transaction-service | Transaction add |
| COBIL00C | payment-service | Bill payment |
| CORPT00C | reporting-service | Reports |
| CBTRN02C | batch-service | Transaction posting |
| CBACT03C | batch-service | Interest calculation |

## License

This project is part of the AWS Mainframe Modernization CardDemo migration.
