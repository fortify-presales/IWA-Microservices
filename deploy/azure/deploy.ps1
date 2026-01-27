# CLI parameters (optional). Values provided via params take precedence over env vars and defaults.
[CmdletBinding(SupportsShouldProcess=$true)]
param(
	[string]$GithubOrganizationParam,
	[string]$DockerUsernameParam,
	[string]$DockerPasswordParam,
	[string]$AzureLocationParam,
	[string]$AzureResourceGroupParam,
	[string]$AzureContainerEnvironmentParam,
	[string]$ImageTagParam,
	[string]$LogsWorkspaceNameParam,
	[string]$ServiceParam
)

# Expose the script's PSCmdlet to functions so they can call ShouldProcess
$script:PSCmdlet = $PSCmdlet

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

# Docker password / token — require explicit param or DOCKER_PASSWORD env var (no fallbacks)
if ($DockerPasswordParam) {
	$DockerPassword = $DockerPasswordParam
} elseif ($env:DOCKER_PASSWORD) {
	$DockerPassword = $env:DOCKER_PASSWORD
} else {
	Write-Error "Docker password not provided. Set -DockerPasswordParam or the DOCKER_PASSWORD environment variable."
	exit 1
}

# Azure settings (allow overrides via env vars)
$AzureLocation = if ($AzureLocationParam) { $AzureLocationParam } elseif ($env:AZURE_LOCATION) { $env:AZURE_LOCATION } else { "uksouth" }
$AzureResourceGroup = if ($AzureResourceGroupParam) { $AzureResourceGroupParam } elseif ($env:AZURE_RESOURCE_GROUP) { $env:AZURE_RESOURCE_GROUP } else { "default-uk-rg" }
$AzureContainerEnvironment = if ($AzureContainerEnvironmentParam) { $AzureContainerEnvironmentParam } elseif ($env:AZURE_CONTAINER_ENVIRONMENT) { $env:AZURE_CONTAINER_ENVIRONMENT } else { "iwa-dev-uk-cae" }

# Logs workspace name (default to log-iwa-platform-dev-uk)
$LogsWorkspaceName = if ($LogsWorkspaceNameParam) { $LogsWorkspaceNameParam } else { "log-iwa-platform-dev-uk" }

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

# Normalize optional Service parameter: accept 'orders' or 'orders-service' or 'gateway'
$TargetService = $null
if ($ServiceParam) {
	$raw = $ServiceParam.ToString().ToLower().Trim()
	if ($raw.EndsWith('-service')) { $raw = $raw.Substring(0, $raw.Length - 8) }
	# Accept 'gateway' as-is
	$TargetService = $raw
	Write-Host "Service parameter specified; targeting only: $TargetService"
}

Write-Host "[1/6] Ensure resource group exists" -ForegroundColor Yellow
# Ensure resource group exists
if ($script:PSCmdlet.ShouldProcess("ResourceGroup: $AzureResourceGroup", "Create resource group in $AzureLocation")) {
	az group create --name $AzureResourceGroup --location $AzureLocation | Out-Null
	Write-Host "Resource group ensured: $AzureResourceGroup" -ForegroundColor Green
} else {
	Write-Host "WhatIf: az group create --name $AzureResourceGroup --location $AzureLocation"
}

Write-Host "[2/6] Ensure Log Analytics workspace exists" -ForegroundColor Yellow
# Ensure Log Analytics workspace exists (create if missing)
# We need the workspace customerId (GUID) for Container Apps --logs-workspace-id
$wsResourceId = az monitor log-analytics workspace show --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --query id -o tsv 2>$null
$wsCustomerId = az monitor log-analytics workspace show --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --query customerId -o tsv 2>$null
if ([string]::IsNullOrEmpty($wsResourceId) -or [string]::IsNullOrEmpty($wsCustomerId)) {
	if ($script:PSCmdlet.ShouldProcess("LogAnalyticsWorkspace: $LogsWorkspaceName", "Create Log Analytics workspace in $AzureResourceGroup/$AzureLocation")) {
		az monitor log-analytics workspace create --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --location $AzureLocation --sku PerGB2018 | Out-Null
		$wsResourceId = az monitor log-analytics workspace show --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --query id -o tsv
		$wsCustomerId = az monitor log-analytics workspace show --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --query customerId -o tsv
		Write-Host "Log Analytics workspace ensured: $LogsWorkspaceName (customerId: $wsCustomerId)" -ForegroundColor Green
	} else {
		Write-Host "WhatIf: az monitor log-analytics workspace create --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --location $AzureLocation --sku PerGB2018"
		$wsResourceId = "<resourceId-from-create:$LogsWorkspaceName>"
		$wsCustomerId = "<customerId-from-create:$LogsWorkspaceName>"
	}
} else {
	Write-Host "Log Analytics workspace already exists: $LogsWorkspaceName (customerId: $wsCustomerId)" -ForegroundColor Green
}

# Retrieve the workspace shared key (required by some CLI versions)
$wsKey = $null
try {
	$wsKey = az monitor log-analytics workspace get-shared-keys --resource-group $AzureResourceGroup --workspace-name $LogsWorkspaceName --query primarySharedKey -o tsv 2>$null
} catch {
	# ignore; some Azure CLI setups may not return a key in WhatIf/dry-run
}

Write-Host "[3/6] Create container apps environment" -ForegroundColor Yellow
# Create container apps environment (may take some time) and pass logs workspace customerId/key
if ($script:PSCmdlet.ShouldProcess("ContainerAppsEnvironment: $AzureContainerEnvironment", "Create container apps environment in $AzureLocation")) {
	if ([string]::IsNullOrEmpty($wsKey)) {
		az containerapp env create --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --location $AzureLocation --logs-workspace-id $wsCustomerId | Out-Null
	} else {
		az containerapp env create --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --location $AzureLocation --logs-workspace-id $wsCustomerId --logs-workspace-key $wsKey | Out-Null
	}
	Write-Host "Container Apps environment ensured: $AzureContainerEnvironment" -ForegroundColor Green
} else {
	if ([string]::IsNullOrEmpty($wsKey)) {
		Write-Host "WhatIf: az containerapp env create --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --location $AzureLocation --logs-workspace-id $wsCustomerId"
	} else {
		Write-Host "WhatIf: az containerapp env create --name $AzureContainerEnvironment --resource-group $AzureResourceGroup --location $AzureLocation --logs-workspace-id $wsCustomerId --logs-workspace-key ****"
	}
}

Write-Host "[4/6] Waiting for environment default domain" -ForegroundColor Yellow
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
} else {
	Write-Host "Default domain: $DefaultDomain" -ForegroundColor Green
}

# Note: Azure Container Apps needs access to private registries. Provide GHCR creds via the --registry-username and --registry-password flags.

function CreateOrUpdate-ServiceApp($name, $port, $imageTag) {
		$base = $name.Split('-')[0]
		$image = "ghcr.io/${GithubOrganization}/iwa-microservices-${base}:${imageTag}"
		$exists = az containerapp show --name $name --resource-group $AzureResourceGroup --query name -o tsv 2>$null
		if ([string]::IsNullOrEmpty($exists)) {
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
					if ($script:PSCmdlet.ShouldProcess("ContainerApp: $name", "Create with image $image")) {
						Write-Host "Creating $name with image $image"
						& az @createArgs | Out-Null
					} else {
						Write-Host "WhatIf: az $($( $createArgs -join ' ' ))"
					}
		} else {
				$updateArgs = @(
					'containerapp','update',
					'--name', $name,
					'--resource-group', $AzureResourceGroup,
					'--image', $image
				)
					# Note: some az CLI versions do not accept registry creds or --env-vars on update.
					if ($script:PSCmdlet.ShouldProcess("ContainerApp: $name", "Update to image $image")) {
						Write-Host "Updating $name to image $image"
						& az @updateArgs | Out-Null
					} else {
						Write-Host "WhatIf: az $($( $updateArgs -join ' ' ))"
					}
		}
}

Write-Host "[5/6] Deploying service apps" -ForegroundColor Yellow
if ([string]::IsNullOrEmpty($TargetService)) {
	# Deploy all services (existing behavior)
	CreateOrUpdate-ServiceApp "catalog-service" 8081 $ImageTag
	CreateOrUpdate-ServiceApp "customers-service" 8082 $ImageTag
	CreateOrUpdate-ServiceApp "orders-service" 8083 $ImageTag
	CreateOrUpdate-ServiceApp "payments-service" 8084 $ImageTag
	CreateOrUpdate-ServiceApp "prescriptions-service" 8085 $ImageTag
	CreateOrUpdate-ServiceApp "inventory-service" 8086 $ImageTag
	CreateOrUpdate-ServiceApp "notifications-service" 8087 $ImageTag
	Write-Host "Service apps deployment complete" -ForegroundColor Green
} else {
	# Deploy only the requested service
	if ($TargetService -eq 'gateway') {
		Write-Host "Deploying only gateway (requested)" -ForegroundColor Yellow
		CreateOrUpdate-Gateway $ImageTag
	} else {
		switch ($TargetService) {
			'catalog' { CreateOrUpdate-ServiceApp "catalog-service" 8081 $ImageTag }
			'customers' { CreateOrUpdate-ServiceApp "customers-service" 8082 $ImageTag }
			'orders' { CreateOrUpdate-ServiceApp "orders-service" 8083 $ImageTag }
			'payments' { CreateOrUpdate-ServiceApp "payments-service" 8084 $ImageTag }
			'prescriptions' { CreateOrUpdate-ServiceApp "prescriptions-service" 8085 $ImageTag }
			'inventory' { CreateOrUpdate-ServiceApp "inventory-service" 8086 $ImageTag }
			'notifications' { CreateOrUpdate-ServiceApp "notifications-service" 8087 $ImageTag }
			Default {
				Write-Host "Unknown service specified: $ServiceParam" -ForegroundColor Yellow
				exit 1
			}
		}
		Write-Host "Requested service deployment complete" -ForegroundColor Green
	}
}

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
		if ($script:PSCmdlet.ShouldProcess('gateway', "Create gateway with image $image")) {
		    Write-Host "Creating gateway with image $image"
		    & az @createArgs | Out-Null
		} else {
		    Write-Host "WhatIf: az $($( $createArgs -join ' ' ))"
		}
	} else {
		$envPairs = $envVars.GetEnumerator() | ForEach-Object { "{0}={1}" -f $_.Key, $_.Value }
				$updateArgs = @(
						'containerapp','update',
						'--name', 'gateway',
						'--resource-group', $AzureResourceGroup,
						'--image', $image
				)
				if ($script:PSCmdlet.ShouldProcess('gateway', "Update gateway to image $image")) {
					Write-Host "Updating gateway to image $image"
					& az @updateArgs | Out-Null
				} else {
					Write-Host "WhatIf: az $($( $updateArgs -join ' ' ))"
				}
	}
}

Write-Host "[6/6] Deploying gateway" -ForegroundColor Yellow
# Only deploy gateway if no specific service requested, or if gateway was explicitly requested
if ([string]::IsNullOrEmpty($TargetService) -or $TargetService -eq 'gateway') {
	CreateOrUpdate-Gateway $ImageTag
} else {
	Write-Host "Skipping gateway deployment (targeted service: $TargetService)" -ForegroundColor Yellow
}

# Show gateway endpoint URL
$gatewayFqdn = az containerapp show --name gateway --resource-group $AzureResourceGroup --query properties.configuration.ingress.fqdn --output tsv 2>$null
if (-not [string]::IsNullOrEmpty($gatewayFqdn)) {
	$gatewayUrl = "https://$gatewayFqdn/swagger-ui.html"
	Write-Host "Gateway URL: $gatewayUrl" -ForegroundColor Cyan
	Write-Host "Deployment complete." -ForegroundColor Green
} else {
	Write-Host "Gateway FQDN not available. Run without -WhatIf to retrieve the gateway URL." -ForegroundColor Yellow
}