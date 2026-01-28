# CardDemo Frontend

React TypeScript frontend for the CardDemo Credit Card Management System, migrated from the mainframe COBOL/CICS application.

## Technology Stack

- **React 19** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router 7** - Client-side routing
- **TanStack Query** - Server state management
- **Zustand** - Client state management
- **Axios** - HTTP client
- **Tailwind CSS** - Styling

## Features

This frontend implements all functionality from the original CardDemo mainframe application:

### Authentication (COSGN00C)
- User login with JWT authentication
- User registration
- Session management with token refresh

### Customer Management (COCRDLIC, COCRDUPC)
- View customer list with pagination
- Search customers by name, SSN, or phone
- Filter customers by state
- Add new customers
- Edit customer details
- Delete customers

### Account Management (COACTVWC, COACTUPC)
- View all accounts with pagination
- Filter by active status or over-limit
- View account details and balances
- Update credit limits
- Activate/deactivate accounts

### Card Management (COCRDSLC, COCRDUPC)
- View all cards with pagination
- Search by last 4 digits
- View expiring cards
- Issue new cards
- Update card details
- Activate/deactivate cards

### Transaction Management (COTRN00C, COTRN01C, COTRN02C)
- View transaction history
- Filter by card number or date range
- View transaction details
- Add new transactions

### Payment Processing (COBIL00C)
- View payment history
- Filter by status (pending, scheduled)
- Make new payments (ACH, debit, check, cash)
- Schedule future payments
- Process or cancel pending payments

### Reports (CORPT00C)
- Dashboard with key metrics
- Account statements with transaction history
- Transaction reports with analytics

## Project Structure

```
src/
├── components/
│   ├── auth/           # Authentication components
│   ├── common/         # Reusable UI components
│   └── layout/         # Layout components
├── pages/              # Page components
├── services/           # API service modules
├── store/              # Zustand state stores
├── types/              # TypeScript type definitions
└── App.tsx             # Main application component
```

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

```bash
npm install
```

### Configuration

Create a `.env` file based on `.env.example`:

```bash
cp .env.example .env
```

Update the API URL if needed:

```
VITE_API_URL=http://localhost:8080/api/v1
```

### Development

```bash
npm run dev
```

The application will be available at http://localhost:3000

### Build

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## API Integration

The frontend integrates with the CardDemo backend microservices through the API Gateway:

| Service | Port | Endpoints |
|---------|------|-----------|
| API Gateway | 8080 | All routes |
| Auth Service | 8081 | /api/v1/auth/* |
| Customer Service | 8082 | /api/v1/customers/* |
| Account Service | 8083 | /api/v1/accounts/* |
| Card Service | 8084 | /api/v1/cards/* |
| Transaction Service | 8085 | /api/v1/transactions/* |
| Payment Service | 8086 | /api/v1/payments/* |
| Reporting Service | 8087 | /api/v1/reports/* |

## COBOL to React Mapping

| COBOL Program | React Component |
|---------------|-----------------|
| COSGN00C | Login.tsx, Register.tsx |
| COCRDLIC | Customers.tsx (list view) |
| COCRDUPC | Customers.tsx (edit modal) |
| COACTVWC | Accounts.tsx (list view) |
| COACTUPC | Accounts.tsx (edit modal) |
| COCRDSLC | Cards.tsx (list view) |
| COCRDUPC | Cards.tsx (edit modal) |
| COTRN00C | Transactions.tsx (list view) |
| COTRN01C | Transactions.tsx (add form) |
| COTRN02C | Transactions.tsx (details) |
| COBIL00C | Payments.tsx |
| CORPT00C | Reports.tsx, Dashboard.tsx |

## Authentication

The application uses JWT-based authentication:

1. User logs in with username/password
2. Backend returns access token and refresh token
3. Access token is included in all API requests
4. When access token expires, refresh token is used to get a new one
5. If refresh fails, user is redirected to login

## State Management

- **Server State**: TanStack Query handles API data fetching, caching, and synchronization
- **Client State**: Zustand manages authentication state with persistence to localStorage

## Styling

Tailwind CSS is used for styling with custom utility classes defined in `index.css`:

- `.btn-primary` - Primary action buttons
- `.btn-secondary` - Secondary action buttons
- `.btn-danger` - Destructive action buttons
- `.input-field` - Form input fields
- `.card` - Card containers
- `.table-header` - Table header cells
