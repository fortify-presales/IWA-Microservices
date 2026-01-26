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

## Using the DevSecOps GitHub Actions workflow

The repository includes a comprehensive GitHub Actions pipeline at `.github/workflows/devsecops.yml` that builds, tests, containerizes, scans (Fortify/Trivy optional) and can deploy the services to Azure Container Apps.

This section explains how to trigger and configure that workflow and how to provide the Azure credentials the workflow needs.

### How the workflow is triggered
- Push events on branches: `main`, `develop`, `feature/**`, `hotfix/**` trigger builds.
- Pull requests targeting `main` and `develop` trigger PR validations.
- You can also trigger the workflow manually from the Actions tab using "Run workflow" (workflow_dispatch).

> Tip: For local CI debugging you can create a temporary branch and push to it, or open a PR against `develop` to exercise the pipeline without affecting `main`.

### Required repository secrets and variables
The workflow reads several repository-level variables and secrets. At minimum, to enable deployment to Azure you must create these GitHub repository secrets:

- `AZURE_TENANT_ID` – Azure Active Directory Tenant ID
- `AZURE_CLIENT_ID` – Azure AD Application (Service Principal) Client ID
- `AZURE_CLIENT_SECRET` – Client secret for the Service Principal
- `AZURE_SUBSCRIPTION_ID` – Azure Subscription ID

Additionally, the pipeline references Fortify (FoD) variables/secrets if you enable the Fortify scan steps (see the top of `.github/workflows/devsecops.yml`). Those are optional and documented in the workflow file.

### Create an Azure Service Principal (recommended steps)
You can create a service principal using the Azure CLI. The example below creates an SP scoped to a subscription and outputs an SDK-auth JSON (useful for automation). Adjust the role and scope to follow least privilege.

1. Login to Azure and set the target subscription (interactive):

```bash
az login
# list subscriptions and copy the ID you want to use
az account list --output table
# set the subscription you want the SP to target
az account set --subscription "<your-subscription-id>"
```

2. Create a service principal scoped to the subscription (example uses Contributor role). For a production setup choose a narrower role (e.g., 'Azure Container Apps Contributor'):

```bash
# Replace <subscription-id> and <unique-name>
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
az ad sp create-for-rbac --name "gha-iwa-microservices-sp" --role Contributor --scopes /subscriptions/${SUBSCRIPTION_ID} --sdk-auth > sp.json
```

This writes an `sp.json` file containing JSON like:

```text
{
  "clientId": "<clientId>",
  "clientSecret": "<clientSecret>",
  "subscriptionId": "<subscriptionId>",
  "tenantId": "<tenantId>",
  "activeDirectoryEndpointUrl": "<activeDirectoryEndpointUrl>",
  "resourceManagerEndpointUrl": "<resourceManagerEndpointUrl>"
}
```

3. Extract the values you need (or open `sp.json` and copy):

- AZURE_CLIENT_ID = clientId
- AZURE_CLIENT_SECRET = clientSecret
- AZURE_TENANT_ID = tenantId
- AZURE_SUBSCRIPTION_ID = subscriptionId

> Security note: Do not commit `sp.json` to source control; treat it as a secret.

### Add the Azure secrets to GitHub
You can add secrets via the GitHub web UI or the `gh` CLI.

Web UI (repository-level):
1. Go to your repository on GitHub.
2. Settings → Secrets and variables → Actions → New repository secret.
3. Add each secret name and value (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID).

Using GitHub CLI (`gh`):

```bash
# Example using values from sp.json (Linux/macOS/WSL / PowerShell adapt as needed)
cat sp.json | jq -r '.clientId' | xargs -I{} gh secret set AZURE_CLIENT_ID --body "{}"
cat sp.json | jq -r '.clientSecret' | xargs -I{} gh secret set AZURE_CLIENT_SECRET --body "{}"
cat sp.json | jq -r '.tenantId' | xargs -I{} gh secret set AZURE_TENANT_ID --body "{}"
cat sp.json | jq -r '.subscriptionId' | xargs -I{} gh secret set AZURE_SUBSCRIPTION_ID --body "{}"
```

(If you don't have `jq`, you can open `sp.json` and copy/paste values into the GitHub web UI.)

### Minimal permissions guidance
- The example creates a Service Principal with `Contributor` rights for convenience. For least privilege, scope it to roles that allow deployment of Container Apps only (for example `Azure Container Apps Contributor` or a custom RBAC role) and scope to the resource group rather than the whole subscription.

### How the workflow uses these secrets
- The pipeline step `Azure Login` (uses `azure/login@v1`) reads `tenant-id`, `client-id` and `subscription-id` from the repository secrets and uses them to authenticate the pipeline for subsequent `az` commands.
- The `deploy-to-azure` job runs only on `main` branch pushes by default (see `.github/workflows/devsecops.yml`).

### Triggering a deploy manually
- Open the Actions tab → choose the `DevSecOps Pipeline` workflow → click `Run workflow` (provide branch and any inputs) to dispatch a run manually.
- Alternatively, push a commit to `main` (or merge a PR) to trigger the full pipeline which includes the `deploy-to-azure` job (when on `main`).

### Troubleshooting
- If `azure/login` fails, verify the SP values, that the client secret has not expired, and that the SP has appropriate RBAC rights for the subscription or resource group.
- For permission errors during deploy, check the assigned role and scope for the SP and consider granting the necessary role at the resource group level rather than subscription-wide.

### Azure authentication options for `azure/login`

Short answer: `AZURE_CLIENT_SECRET` is only required when you authenticate using a Service Principal + client secret; it is not required when using GitHub's OIDC (workload identity federation) flow.

Details:

- Option 1 — OIDC (recommended)
  - Use GitHub Actions' OIDC issuance to exchange a short-lived token for an Azure access token. No long-lived client secret stored in GitHub.
  - Requirements: your workflow must include `permissions: id-token: write` (the repository already sets this). Create an Azure AD App Registration and add a Federated Identity Credential that trusts GitHub for your repo/branch.
  - Example `azure/login` step (no client secret required):

```yaml
- name: Azure Login (OIDC)
  uses: azure/login@v1
  with:
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

  - Benefits: no long-lived secret in GitHub; simpler rotation and better security posture.

- Option 2 — Service Principal + client secret (classic)
  - If you prefer or must use a client secret, create a Service Principal and store `AZURE_CLIENT_SECRET` as a GitHub secret.
  - Example `azure/login` step using a client secret:

```yaml
- name: Azure Login (client secret)
  uses: azure/login@v1
  with:
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    client-secret: ${{ secrets.AZURE_CLIENT_SECRET }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

  - Create the SP and obtain credentials:

```bash
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
az ad sp create-for-rbac --name "gha-iwa-microservices-sp" --role Contributor --scopes /subscriptions/${SUBSCRIPTION_ID} --sdk-auth > sp.json
```

  - `sp.json` contains `clientId`, `clientSecret`, `tenantId`, and `subscriptionId`.

  - Add `AZURE_CLIENT_SECRET` to GitHub:
    - Web UI: Repository → Settings → Secrets and variables → Actions → New repository secret → name `AZURE_CLIENT_SECRET` and paste the value.
    - gh CLI example:

```bash
gh secret set AZURE_CLIENT_SECRET --body "$(jq -r '.clientSecret' sp.json)"
```

Recommendations
- Prefer OIDC / federated credentials when possible. If you must use client secrets, rotate them regularly and scope the SP's RBAC to least privilege (resource group and narrow role such as `Azure Container Apps Contributor`).

## Azure Deployment

See `deploy/azure/README.md` for Azure Container Apps deployment instructions.

### Deploy script updates

- **WhatIf preview:** `deploy/azure/deploy.ps1` now supports `-WhatIf` to preview the `az` commands the script would run without making changes.
- **Logs workspace:** the script accepts `-LogsWorkspaceName` (default: `log-iwa-platform-dev-uk`) and will create a Log Analytics workspace (`--sku PerGB2018`) if missing. It retrieves the workspace `customerId` (GUID) and (when available) the shared key and passes these to the Container Apps environment using `--logs-workspace-id` (customerId) and `--logs-workspace-key`. WhatIf output masks keys.
- **Behavior changes for updates:** for CLI compatibility the script no longer passes registry credentials or `--env-vars` on `az containerapp update` (it only updates `--image`). Creation commands still include registry creds and env vars such as `OPENAPI_SERVER_URL`.
- **Progress output & gateway URL:** the script prints colored, numbered step headings and shows the gateway Swagger URL at the end (e.g. `https://<gatewayFqdn>/swagger-ui.html`).
- **CORS (insecure demo change):** to allow the gateway Swagger UI to fetch each service's `/v3/api-docs` the services in this demo have an intentionally permissive CORS configuration (`CorsConfig` mapping `/**` -> `*`). THIS IS INSECURE — it is done for demo convenience only. To harden, remove or restrict the `CorsConfig` classes under `services/*/src/main/java/.../config/CorsConfig.java`.

Recommended local rebuild & deploy steps after code changes:

```bash
./gradlew build
# Build/push images (example; adjust tags/org)
docker build -t ghcr.io/<org>/iwa-microservices-catalog:main ./services/catalog
docker push ghcr.io/<org>/iwa-microservices-catalog:main
# repeat for each service, then run deploy script
powershell .\deploy\azure\deploy.ps1
```

Notes: you must be signed into Azure CLI with the correct subscription and have permissions to create resource groups and Log Analytics workspaces.

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

## Postman collection (quick demo)

A ready-made Postman collection is included for the Customers service to exercise login, register, update and token validation flows.

Files (relative to repo root):
- `services/customers/postman/CustomersService.postman_collection.json` (the collection)
- `services/customers/postman/CustomersService.postman_environment.json` (postman environment with `username`/`password` defaults)
- `services/customers/postman/package.json` (helper npm script to run Newman)

Run the collection (quick, no install):

```powershell
cd services\customers\postman
npx newman run CustomersService.postman_collection.json -e CustomersService.postman_environment.json --delay-request 50 --reporters cli
```

Or install and run via npm script:

```powershell
cd services\customers\postman
npm install
npm run run-collection
```

Notes
- The environment defaults to `username = john.doe` and `password = password123` (seeded in the demo DB). If you prefer another user, edit the environment file or update variables in the Postman GUI.
- The collection stores `token` (full `Bearer <jwt>`) and `rawToken` (compact JWT) after login; the validate request uses `rawToken` to avoid base64 decoding errors.
