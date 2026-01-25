# CLI parameters (optional). Values provided via params take precedence over env vars and defaults.
param(
	[string]$GithubOrganizationParam,
	[string]$DockerUsernameParam,
	[string]$DockerPasswordParam,
	[string]$AzureLocationParam,
	[string]$AzureResourceGroupParam,
	[string]$AzureContainerEnvironmentParam,
	[string]$ImageTagParam
)

# Configuration (allow overrides from GitHub Actions / env vars)
# Order of precedence: explicit env var -> common GH Actions vars -> hardcoded default

# GitHub organization (env: GITHUB_ORGANIZATION) or derived from GITHUB_REPOSITORY (owner/repo)
$envRepo = $env:GITHUB_REPOSITORY
if ($GithubOrganizationParam) {
	$GithubOrganization = $GithubOrganizationParam
} elseif ($env:GITHUB_ORGANIZATION) {
	$GithubOrganization = $env:GITHUB_ORGANIZATION
} elseif ($envRepo) {
	$GithubOrganization = ($envRepo -split '/')[0]
} else {
	$GithubOrganization = "fortify-presales"
}

# Docker username (env: DOCKER_USERNAME) or GitHub actor
if ($DockerUsernameParam) { $DockerUsername = $DockerUsernameParam } elseif ($env:DOCKER_USERNAME) { $DockerUsername = $env:DOCKER_USERNAME } elseif ($env:GITHUB_ACTOR) { $DockerUsername = $env:GITHUB_ACTOR } else { $DockerUsername = "kadraman" }

# Docker password / token (try common names used in CI)
$DockerPassword = $null
if ($DockerPasswordParam) {
	$DockerPassword = $DockerPasswordParam
} else {
	$DockerPassword = $env:DOCKER_PASSWORD
	if (-not $DockerPassword) { $DockerPassword = $env:CR_PAT }
	if (-not $DockerPassword) { $DockerPassword = $env:GHCR_PAT }
	if (-not $DockerPassword) { $DockerPassword = $env:GITHUB_TOKEN }
	if (-not $DockerPassword) { $DockerPassword = "<YOUR_GITHUB_PERSONAL_ACCESS_TOKEN>" }
}

# Azure settings (allow overrides via env vars)
$AzureLocation = if ($AzureLocationParam) { $AzureLocationParam } elseif ($env:AZURE_LOCATION) { $env:AZURE_LOCATION } else { "uksouth" }
$AzureResourceGroup = if ($AzureResourceGroupParam) { $AzureResourceGroupParam } elseif ($env:AZURE_RESOURCE_GROUP) { $env:AZURE_RESOURCE_GROUP } else { "default-uk-rg" }
$AzureContainerEnvironment = if ($AzureContainerEnvironmentParam) { $AzureContainerEnvironmentParam } elseif ($env:AZURE_CONTAINER_ENVIRONMENT) { $env:AZURE_CONTAINER_ENVIRONMENT } else { "iwa-dev-uk-cae" }

# Image tag to deploy: prefer IMAGE_TAG, then try to infer from GITHUB_REF (branch), else default to main
if ($ImageTagParam) {
	$ImageTag = $ImageTagParam
} elseif ($env:IMAGE_TAG) {
	$ImageTag = $env:IMAGE_TAG
} elseif ($env:GITHUB_REF) {
	# GITHUB_REF may be refs/heads/<branch> or refs/tags/<tag>
	if ($env:GITHUB_REF -match 'refs/(heads|tags)/(.+)$') { $ImageTag = $matches[2] } else { $ImageTag = "main" }
} else {
	$ImageTag = "main"
}

# Ensure resource group exists
az group create --name $AzureResourceGroup --location $AzureLocation | Out-Null

# Create container apps environment (may take some time)
az containerapp env create --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --location $AzureLocation | Out-Null

# Wait for the environment to report a default domain (poll with timeout)
$DefaultDomain = ""
$maxRetries = 30
$i = 0
while ([string]::IsNullOrEmpty($DefaultDomain) -and $i -lt $maxRetries) {
		Start-Sleep -Seconds 5
		$DefaultDomain = (az containerapp env show --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --query properties.defaultDomain -o tsv) -as [string]
		$i++
}
if ([string]::IsNullOrEmpty($DefaultDomain)) {
		Write-Error "Timed out waiting for container apps environment default domain"
		exit 1
}

# Note: Azure Container Apps needs access to private registries. Provide GHCR creds via the --registry-username and --registry-password flags.

function CreateOrUpdate-ServiceApp($name, $port, $imageTag) {
		$base = $name.Split('-')[0]
		$image = "ghcr.io/${GithubOrganization}/iwa-microservices-${base}:${imageTag}"
		$exists = az containerapp show --name $name --resource-group $AzureResourceGroup --query name -o tsv 2>$null
		if ([string]::IsNullOrEmpty($exists)) {
				Write-Host "Creating $name with image $image"
				$createArgs = @(
					'containerapp','create',
					'--name', $name,
					'--resource-group', $AzureResourceGroup,
					'--environment', $AzureContainerEnvironment,
					'--image', $image,
					'--target-port', $port,
					'--ingress', 'external',
					'--cpu', '0.5',
					'--memory', '1.0Gi',
					'--registry-server', 'ghcr.io',
					'--registry-username', $DockerUsername,
					'--registry-password', $DockerPassword
				)
					# Add OPENAPI_SERVER_URL env var so OpenAPI reports public URL in Azure
					$openApiUrl = "https://$name.$DefaultDomain"
					$createArgs += @('--env-vars', "OPENAPI_SERVER_URL=$openApiUrl")
					& az @createArgs | Out-Null
		} else {
				Write-Host "Updating $name to image $image"
				$updateArgs = @(
					'containerapp','update',
					'--name', $name,
					'--resource-group', $AzureResourceGroup,
					'--image', $image,
					'--registry-server', 'ghcr.io',
					'--registry-username', $DockerUsername,
					'--registry-password', $DockerPassword
				)
					# Ensure OPENAPI_SERVER_URL is set/updated on the app
					$openApiUrl = "https://$name.$DefaultDomain"
					$updateArgs += @('--env-vars', "OPENAPI_SERVER_URL=$openApiUrl")
					& az @updateArgs | Out-Null
		}
}

CreateOrUpdate-ServiceApp "catalog-service" 8081 $ImageTag
CreateOrUpdate-ServiceApp "customers-service" 8082 $ImageTag
CreateOrUpdate-ServiceApp "orders-service" 8083 $ImageTag
CreateOrUpdate-ServiceApp "payments-service" 8084 $ImageTag
CreateOrUpdate-ServiceApp "prescriptions-service" 8085 $ImageTag
CreateOrUpdate-ServiceApp "inventory-service" 8086 $ImageTag
CreateOrUpdate-ServiceApp "notifications-service" 8087 $ImageTag

# Create gateway and pass environment variables pointing at the services
function CreateOrUpdate-Gateway($imageTag) {
		$image = "ghcr.io/${GithubOrganization}/iwa-microservices-gateway:${imageTag}"
		$envVars = @{
				SERVICES_CATALOG_URL = "https://catalog-service.$DefaultDomain"
				SERVICES_CUSTOMERS_URL = "https://customers-service.$DefaultDomain"
				SERVICES_ORDERS_URL = "https://orders-service.$DefaultDomain"
				SERVICES_PAYMENTS_URL = "https://payments-service.$DefaultDomain"
				SERVICES_PRESCRIPTIONS_URL = "https://prescriptions-service.$DefaultDomain"
				SERVICES_INVENTORY_URL = "https://inventory-service.$DefaultDomain"
				SERVICES_NOTIFICATIONS_URL = "https://notifications-service.$DefaultDomain"
		}
		$exists = az containerapp show --name gateway --resource-group $AzureResourceGroup --query name -o tsv 2>$null
		if ([string]::IsNullOrEmpty($exists)) {
				Write-Host "Creating gateway with image $image"
				# Build env-vars as single string of key=value pairs
				$envPairs = $envVars.GetEnumerator() | ForEach-Object { "{0}={1}" -f $_.Key, $_.Value }
				$createArgs = @(
					'containerapp','create',
					'--name', 'gateway',
					'--resource-group', $AzureResourceGroup,
					'--environment', $AzureContainerEnvironment,
					'--image', $image,
					'--target-port', '8080',
					'--ingress', 'external',
					'--cpu', '0.5',
					'--memory', '1.0Gi',
					'--registry-server', 'ghcr.io',
					'--registry-username', $DockerUsername,
					'--registry-password', $DockerPassword,
					'--env-vars'
				) + ($envPairs)
				& az @createArgs | Out-Null
		} else {
				Write-Host "Updating gateway to image $image and env vars"
				$envPairs = $envVars.GetEnumerator() | ForEach-Object { "{0}={1}" -f $_.Key, $_.Value }
				$updateArgs = @(
						'containerapp','update',
						'--name', 'gateway',
						'--resource-group', $AzureResourceGroup,
						'--image', $image,
						'--registry-server', 'ghcr.io',
						'--registry-username', $DockerUsername,
						'--registry-password', $DockerPassword,
						'--env-vars'
				) + ($envPairs)
				& az @updateArgs | Out-Null
		}
}

CreateOrUpdate-Gateway $ImageTag

az containerapp show --name gateway --resource-group $AzureResourceGroup --query properties.configuration.ingress.fqdn --output tsv