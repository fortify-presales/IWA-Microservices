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

If you'd like, I can:

- Add a CLI parameter to `example.ps1` for `$ImageTag` and other values.
- Show an example GitHub Actions workflow that builds, pushes images to GHCR, and runs this script with secrets.

