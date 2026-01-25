# Azure Container Apps deployment

This directory contains an example deployment script and notes for deploying the IWA-Microservices set to Azure Container Apps.

Important: prefer running the provided PowerShell script `example.ps1` rather than running manual `az containerapp create` commands. The script handles resource-group creation, waiting for the Container Apps environment domain, and create-or-update semantics for Container Apps.

Prerequisites

- Azure CLI installed and logged in (`az login`)
- `az` extensions for container apps installed (usually `az extension add --name containerapp`)
- Images pushed to a registry accessible by Azure (example uses GitHub Container Registry `ghcr.io`)
- A GitHub PAT (or alternative) with `read:packages` if using GHCR for private images

Quick usage

1. Edit `deploy/azure/example.ps1` and set the following variables at the top (or set them in your CI):

```powershell
$GithubOrganization = "fortify-presales"
$DockerUsername = "<your-ghcr-username>"
$DockerPassword = "<GHCR_PAT-or-secret>"  # prefer secrets in CI or Key Vault
$AzureLocation = "uksouth"
$AzureResourceGroup = "default-uk-rg"
$AzureContainerEnvironment = "iwa-dev-uk-cae"
$ImageTag = "main"   # tag to create or update
```

2. Run the script from PowerShell (it will create the resource group, environment, and container apps, or update them if they already exist):

```powershell
.\example.ps1
```

Or pass a different tag by editing `$ImageTag` or setting it before running.

What the script does

- Creates the resource group with `az group create` (idempotent).
- Creates the Container Apps environment and polls for the environment's default domain.
- For each microservice it runs a create-or-update using `az containerapp create` or `az containerapp update` and provides registry credentials so Azure can pull private GHCR images.
- Creates/updates the gateway and injects environment variables pointing to the other services using the Container Apps environment domain.

Security and CI recommendations

- Do NOT store `$DockerPassword` as plaintext in repo. In CI pipelines use secret variables or Azure Key Vault.
- For production, prefer a managed identity or service principal to give Azure access to container registry (or use Azure Container Registry with managed identity).
- Use `az containerapp update --image` to roll out new versions; the script does this when an app already exists.

Manual commands (only if you need them)

If you must run commands manually, the important parts are:

```bash
# create RG
az group create --name MyRg --location uksouth

# create env (one-off)
az containerapp env create --name MyEnv --resource-group MyRg --location uksouth

# create a container app (example)
az containerapp create \
  --name catalog-service \
  --resource-group MyRg \
  --environment MyEnv \
  --image ghcr.io/<org>/iwa-microservices-catalog:<tag> \
  --target-port 8081 --ingress external --cpu 0.5 --memory 1.0Gi \
  --registry-server ghcr.io --registry-username <user> --registry-password <pat>

# update an existing app to a new image
az containerapp update --name catalog-service --resource-group MyRg --image ghcr.io/<org>/iwa-microservices-catalog:<tag> --registry-server ghcr.io --registry-username <user> --registry-password <pat>

# get gateway FQDN
az containerapp show --name gateway --resource-group MyRg --query properties.configuration.ingress.fqdn -o tsv
```

Cleanup

To remove everything created for this demo (resource-group delete):

```bash
az group delete --name MyRg --yes --no-wait
```

### Swagger / OpenAPI - Environment variables for Azure

When deployed to Azure, set these environment variables so the OpenAPI docs and the gateway aggregator point to the public service URLs.

- Per-service OpenAPI server override (set on each service container app):
  - `OPENAPI_SERVER_URL` — full public URL for the service (example: `https://customers-service.<env>.azurecontainerapps.io`). When set, the service will show this URL in its OpenAPI `servers` list.

- Gateway aggregator (set on the gateway container app):
  - `SERVICES_CATALOG_URL` — base URL for Catalog service (no `/v3/api-docs` suffix)
  - `SERVICES_CUSTOMERS_URL`
  - `SERVICES_ORDERS_URL`
  - `SERVICES_PAYMENTS_URL`
  - `SERVICES_PRESCRIPTIONS_URL`
  - `SERVICES_INVENTORY_URL`
  - `SERVICES_NOTIFICATIONS_URL`

Example: if your deployed Customers service is available at `https://customers-service.greenwater-xxxx.uksouth.azurecontainerapps.io` then set for that container app:

```
OPENAPI_SERVER_URL=https://customers-service.greenwater-xxxx.uksouth.azurecontainerapps.io
```

And set the gateway `SERVICES_CUSTOMERS_URL` to the same base URL (the gateway will append `/v3/api-docs`):

```
SERVICES_CUSTOMERS_URL=https://customers-service.greenwater-xxxx.uksouth.azurecontainerapps.io
```

Azure CLI example — create/update a service with `OPENAPI_SERVER_URL` set:

```powershell
az containerapp create \
  --name customers-service \
  --resource-group MyRg \
  --environment MyEnv \
  --image ghcr.io/<org>/iwa-microservices-customers:<tag> \
  --ingress external --target-port 8082 \
  --env-vars OPENAPI_SERVER_URL="https://customers-service.<env>.azurecontainerapps.io" \
  --registry-server ghcr.io --registry-username <user> --registry-password <pat>
```

Azure CLI example — create/update the gateway with service URL env vars (gateway uses these to fetch `/v3/api-docs`):

```powershell
az containerapp create \
  --name api-gateway \
  --resource-group MyRg \
  --environment MyEnv \
  --image ghcr.io/<org>/iwa-microservices-gateway:<tag> \
  --ingress external --target-port 8080 \
  --env-vars \
    SERVICES_CATALOG_URL="https://catalog-service.<env>.azurecontainerapps.io" \
    SERVICES_CUSTOMERS_URL="https://customers-service.<env>.azurecontainerapps.io" \
    SERVICES_ORDERS_URL="https://orders-service.<env>.azurecontainerapps.io" \
    SERVICES_PAYMENTS_URL="https://payments-service.<env>.azurecontainerapps.io" \
    SERVICES_PRESCRIPTIONS_URL="https://prescriptions-service.<env>.azurecontainerapps.io" \
    SERVICES_INVENTORY_URL="https://inventory-service.<env>.azurecontainerapps.io" \
    SERVICES_NOTIFICATIONS_URL="https://notifications-service.<env>.azurecontainerapps.io" \
  --registry-server ghcr.io --registry-username <user> --registry-password <pat>
```

Notes:
- Use secure secrets for registry credentials (CI secrets or Azure Key Vault) instead of passing plain text credentials.
- If your apps are behind a TLS-terminating proxy, ensure `server.forward-headers-strategy=framework` is enabled in the service `application.properties` or configure `OPENAPI_SERVER_URL` explicitly.

After these are set, the gateway aggregated Swagger UI (`/swagger-ui.html` on the gateway FQDN) will load each service's OpenAPI JSON from the public URLs.

