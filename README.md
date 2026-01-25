# IWA-Microservices

A deliberately insecure microservices application for application security testing, DevSecOps demonstrations, and security training.

⚠️ **WARNING**: This application contains intentional security vulnerabilities. DO NOT deploy to production or expose to the public internet.

## Overview

IWA-Microservices is a Java-based microservices pharmacy application designed to demonstrate common security vulnerabilities in modern distributed systems. It serves as a training platform for:

- Application Security Testing (SAST, DAST, SCA)
- DevSecOps pipeline integration
- Security vulnerability identification and remediation
- Secure coding practices education

## Architecture

The application consists of:

### Services
- **Catalog Service** (8081) - Product catalog with SQL injection vulnerabilities
- **Customers Service** (8082) - User authentication with weak security (plain text passwords, JWT issues)
- **Orders Service** (8083) - Order management with insecure deserialization and IDOR
- **Payments Service** (8084) - Payment processing with hardcoded secrets
- **Prescriptions Service** (8085) - Prescription management with IDOR vulnerabilities
- **Inventory Service** (8086) - Stock management with XXE vulnerabilities
- **Notifications Service** (8087) - Email/SMS notifications with command injection

### Applications
- **API Gateway** (8080) - Routes requests to microservices (no authentication/authorization)
- **Frontend SPA** (planned) - React-based user interface

### Shared Libraries
- **Contracts** - Common domain models and DTOs

## Intentional Vulnerabilities

This application deliberately includes the following security issues:

1. **SQL Injection** - String concatenation in database queries (Catalog Service)
2. **Weak Authentication** - Plain text passwords, hardcoded JWT secrets (Customers Service)
3. **Insecure Deserialization** - Unsafe object deserialization (Orders Service)
4. **Hardcoded Secrets** - API keys and credentials in code/config (Payments Service)
5. **IDOR (Insecure Direct Object References)** - No authorization checks (Orders, Prescriptions)
6. **XXE (XML External Entity)** - Unsafe XML parsing (Inventory Service)
7. **Command Injection** - Unsanitized input in system commands (Notifications Service)
8. **Permissive CORS** - No origin validation
9. **Exposed Debug Endpoints** - H2 Console, Actuator endpoints
10. **Logging Sensitive Data** - Passwords and payment info in logs

## Getting Started

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose (for containerized deployment)
- Gradle (wrapper included)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/fortify-presales/IWA-Microservices.git
   cd IWA-Microservices
   ```

2. **Build all services**
   ```bash
   ./gradlew build
   ```

3. **Run individual services**
   ```bash
   # Catalog Service
   ./gradlew :services:catalog:bootRun
   
   # Customers Service
   ./gradlew :services:customers:bootRun
   
   # Orders Service
   ./gradlew :services:orders:bootRun
   
   # ... and so on
   ```

4. **Or use Docker Compose**
   ```bash
   docker-compose up --build
   ```

### Access Services

- **API Gateway**: http://localhost:8080
- **Catalog Service**: http://localhost:8081/api/products
- **Customers Service**: http://localhost:8082/api/customers
- **Orders Service**: http://localhost:8083/api/orders
- **Payments Service**: http://localhost:8084/api/payments
- **Prescriptions Service**: http://localhost:8085/api/prescriptions
- **Inventory Service**: http://localhost:8086/api/inventory
- **Notifications Service**: http://localhost:8087/api/notifications

### H2 Console Access

Each service with a database has an exposed H2 console:
- Catalog: http://localhost:8081/h2-console
- Customers: http://localhost:8082/h2-console
- Orders: http://localhost:8083/h2-console
- Prescriptions: http://localhost:8085/h2-console
- Inventory: http://localhost:8086/h2-console

JDBC URL: `jdbc:h2:mem:<servicename>db` (e.g., `jdbc:h2:mem:catalogdb`)
Username: `sa`
Password: (empty)

### API Documentation (Swagger / OpenAPI)

Each service exposes OpenAPI JSON at `/v3/api-docs` and a Swagger UI. After starting services, access documentation at:

- API Gateway aggregated Swagger UI: http://localhost:8080/swagger-ui.html (lists all services)
- Catalog Service Swagger UI: http://localhost:8081/swagger-ui.html
- Customers Service Swagger UI: http://localhost:8082/swagger-ui.html
- Orders Service Swagger UI: http://localhost:8083/swagger-ui.html
- Payments Service Swagger UI: http://localhost:8084/swagger-ui.html
- Prescriptions Service Swagger UI: http://localhost:8085/swagger-ui.html
- Inventory Service Swagger UI: http://localhost:8086/swagger-ui.html
- Notifications Service Swagger UI: http://localhost:8087/swagger-ui.html

If services run on different ports, update the gateway service URLs in `apps/gateway/src/main/resources/application.properties` to point to the running service instances.

## Testing Vulnerabilities

### SQL Injection (Catalog Service)

```bash
# Search with SQL injection
curl "http://localhost:8081/api/products/search?q=test' OR '1'='1"

# Sort with SQL injection
curl "http://localhost:8081/api/products?sortBy=name;DROP%20TABLE%20products;--&order=ASC"
```

### Authentication Bypass (Customers Service)

```bash
# SQL injection in login
curl -X POST http://localhost:8082/api/customers/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"x' OR '1'='1"}'

# Plaintext passwords visible
curl http://localhost:8082/api/customers/1
```

### IDOR (Orders & Prescriptions)

```bash
# Access other users' orders
curl http://localhost:8083/api/orders/1
curl http://localhost:8083/api/orders/2

# Access other users' prescriptions
curl http://localhost:8085/api/prescriptions/1
```

### XXE (Inventory Service)

```bash
# XXE attack
curl -X POST http://localhost:8086/api/inventory/import \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<inventory><item><productId>&xxe;</productId><quantity>1</quantity></item></inventory>'
```

### Command Injection (Notifications Service)

```bash
# Command injection via email
curl -X POST http://localhost:8087/api/notifications/email \
  -H "Content-Type: application/json" \
  -d '{"to":"test@example.com; id","subject":"Test","body":"Message"}'
```

### Hardcoded Secrets (Payments Service)

```bash
# View exposed configuration
curl http://localhost:8084/api/payments/config
```

## CI/CD Pipeline

The application includes a GitHub Actions workflow that:

1. Builds all services
2. Runs tests
3. Builds Docker images
4. Pushes images to GitHub Container Registry
5. Runs Security Scan (Fortify)
6. Deploys to Azure Container Apps (when configured)

See `.github/workflows/devsecops.yml` for details.

## Azure Deployment

See `deploy/azure/README.md` for Azure Container Apps deployment instructions.

## Project Structure

```
IWA-Microservices/
├── apps/
│   └── gateway/              # API Gateway service
├── services/
│   ├── catalog/              # Product catalog service
│   ├── customers/            # Customer & auth service
│   ├── orders/               # Order management service
│   ├── payments/             # Payment processing service
│   ├── prescriptions/        # Prescription management service
│   ├── inventory/            # Inventory management service
│   └── notifications/        # Notification service
├── libs/
│   └── contracts/            # Shared domain models
├── deploy/
│   └── azure/                # Azure deployment configs
├── .github/
│   └── workflows/            # CI/CD pipelines
├── build.gradle              # Root build configuration
├── settings.gradle           # Multi-module settings
├── docker-compose.yml        # Local Docker deployment
└── README.md                 # This file
```

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2.x** - Application framework
- **Spring Cloud Gateway** - API Gateway
- **H2 Database** - In-memory database
- **Gradle** - Build tool
- **Docker** - Containerization
- **GitHub Actions** - CI/CD
- **Azure Container Apps** - Cloud deployment platform

## Security Testing Recommendations

Use this application to test various security tools:

1. **SAST Tools**: SonarQube, Checkmarx, Fortify, Semgrep
2. **DAST Tools**: OWASP ZAP, Burp Suite, Acunetix
3. **SCA Tools**: Snyk, Dependabot, WhiteSource
4. **Container Scanning**: Trivy, Clair, Anchore
5. **Secrets Detection**: TruffleHog, GitLeaks, detect-secrets

## Educational Use

This application is ideal for:

- Security training workshops
- DevSecOps demonstrations
- Application security tool evaluations
- Secure coding practice
- Vulnerability research and testing

## Support and Contribution

This is a demonstration/training application. For questions or contributions:

- Open an issue on GitHub
- Submit a pull request
- Contact: info@kadrman.com

## License

See LICENSE file for details.

## Disclaimer

⚠️ **SECURITY WARNING**: This application is intentionally vulnerable and insecure. It is designed for educational and testing purposes only. Never deploy this application in a production environment or expose it to untrusted networks. The authors are not responsible for any misuse or damage caused by this application.
