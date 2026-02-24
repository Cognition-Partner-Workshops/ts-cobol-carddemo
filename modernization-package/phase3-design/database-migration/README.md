# Database Migration Scripts (Flyway)

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 3 - Design
> **Migration Tool:** Flyway
> **Target Database:** PostgreSQL 15+

## Migration Order

Scripts must be executed in version order. Flyway handles this automatically.

| Version | File | Description | Dependencies |
|---|---|---|---|
| V1 | `V1__create_sequence.sql` | Transaction ID sequence (`transaction_id_seq`) | None |
| V2 | `V2__create_customer_table.sql` | Customer table (replaces CUSTDAT VSAM) | None |
| V3 | `V3__create_account_table.sql` | Account table (replaces ACCTDAT VSAM) | None |
| V4 | `V4__create_card_table.sql` | Card table (replaces CARDDAT VSAM) | V3 (account FK) |
| V5 | `V5__create_card_cross_reference_table.sql` | Cross-reference table (replaces CCXREF + CXACAIX) | V2, V3, V4 |
| V6 | `V6__create_transaction_table.sql` | Transaction table (replaces TRANSACT VSAM) | V1, V4 |
| V7 | `V7__seed_data.sql` | Sample data for development and testing | V1-V6 |

## VSAM-to-Table Mapping

| VSAM File | Type | PostgreSQL Table | Migration |
|---|---|---|---|
| CUSTDAT | KSDS | `customer` | V2 |
| ACCTDAT | KSDS | `account` | V3 |
| CARDDAT | KSDS | `card` | V4 |
| CCXREF | KSDS | `card_cross_reference` | V5 |
| CXACAIX | AIX | `idx_xref_account_id` (index) | V5 |
| TRANSACT | KSDS | `transaction` | V6 |

## Running Migrations

### With Spring Boot (automatic)

Add to `application.yml`:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Copy migration scripts to `src/main/resources/db/migration/`.

### With Flyway CLI (manual)

```bash
flyway -url=jdbc:postgresql://localhost:5432/carddemo \
       -user=carddemo \
       -password=carddemo \
       -locations=filesystem:./database-migration \
       migrate
```

### With Docker Compose

```bash
docker compose up -d postgres
flyway migrate
```
