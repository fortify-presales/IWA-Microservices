# Azure Container Apps Deployment

This directory contains configuration files for deploying the IWA-Microservices microservices to Azure Container Apps.

## Prerequisites

1. Azure CLI installed
2. Azure subscription
3. Docker images built and pushed to a container registry

## Deployment Steps

### 1. Create Resource Group

```bash
az group create \
  --name iwa-microservices-rg \
  --location eastus
```

### 2. Create Container Apps Environment

```bash
az containerapp env create \
  --name iwa-pharmacy-env \
  --resource-group iwa-microservices-rg \
  --location eastus
```

### 3. Deploy Microservices

Deploy each microservice as a container app:

```bash
# Catalog Service
az containerapp create \
  --name catalog-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-catalog:latest \
  --target-port 8081 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Customers Service
az containerapp create \
  --name customers-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-customers:latest \
  --target-port 8082 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Orders Service
az containerapp create \
  --name orders-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-orders:latest \
  --target-port 8083 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Payments Service
az containerapp create \
  --name payments-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-payments:latest \
  --target-port 8084 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Prescriptions Service
az containerapp create \
  --name prescriptions-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-prescriptions:latest \
  --target-port 8085 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Inventory Service
az containerapp create \
  --name inventory-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-inventory:latest \
  --target-port 8086 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# Notifications Service
az containerapp create \
  --name notifications-service \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-notifications:latest \
  --target-port 8087 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi

# API Gateway
az containerapp create \
  --name gateway \
  --resource-group iwa-microservices-rg \
  --environment iwa-pharmacy-env \
  --image ghcr.io/kadraman/iwa-microservices-gateway:latest \
  --target-port 8080 \
  --ingress external \
  --cpu 0.5 --memory 1.0Gi \
  --env-vars \
    SERVICES_CATALOG_URL=https://catalog-service.ENVIRONMENT_DOMAIN \
    SERVICES_CUSTOMERS_URL=https://customers-service.ENVIRONMENT_DOMAIN \
    SERVICES_ORDERS_URL=https://orders-service.ENVIRONMENT_DOMAIN \
    SERVICES_PAYMENTS_URL=https://payments-service.ENVIRONMENT_DOMAIN \
    SERVICES_PRESCRIPTIONS_URL=https://prescriptions-service.ENVIRONMENT_DOMAIN \
    SERVICES_INVENTORY_URL=https://inventory-service.ENVIRONMENT_DOMAIN \
    SERVICES_NOTIFICATIONS_URL=https://notifications-service.ENVIRONMENT_DOMAIN
```

Replace `ENVIRONMENT_DOMAIN` with your actual Container Apps environment domain.

### 4. Get Gateway URL

```bash
az containerapp show \
  --name gateway \
  --resource-group iwa-microservices-rg \
  --query properties.configuration.ingress.fqdn \
  --output tsv
```

## Infrastructure as Code (Bicep)

For automated deployment, use the Bicep templates in this directory:

```bash
az deployment group create \
  --resource-group iwa-microservices-rg \
  --template-file main.bicep \
  --parameters @parameters.json
```

## Security Warning

⚠️ **WARNING**: This application contains intentional security vulnerabilities and should NEVER be deployed to a production environment accessible from the public internet without proper security controls and isolation.

## Cleanup

To remove all resources:

```bash
az group delete --name iwa-microservices-rg --yes --no-wait
```
