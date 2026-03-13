# Customers Service

## Overview

This service is protected by an OAuth2 Resource Server.
Get access tokens from the auth server and call customers APIs with Bearer tokens.

## Prerequisites
- Java 17
- PowerShell
- Run auth-server, gateway, and customers services
- Node.js and npm (for running the Postman collection with Newman)

## Start services

```powershell
# From repository root, in separate terminals
.\gradlew :apps:auth-server:bootRun
.\gradlew :apps:gateway:bootRun
.\gradlew :services:customers:bootRun
```

## Get an access token (client credentials)

```powershell
$tokenResp = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:9000/oauth2/token' `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body 'grant_type=client_credentials&client_id=gateway-client&client_secret=gateway-secret&scope=customers.read customers.write'

$accessToken = $tokenResp.access_token
```

## Call customers APIs via gateway

```powershell
# Read customers (requires customers.read)
Invoke-RestMethod -Method Get `
  -Uri 'http://localhost:8080/api/customers/2' `
  -Headers @{ Authorization = "Bearer $accessToken" }

# Update customer (requires customers.write)
$update = @{ username='john.doe'; email='john.doe@example.com'; firstName='John'; lastName='Doe' } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/customers/2' `
  -Headers @{ Authorization = "Bearer $accessToken" } `
  -ContentType 'application/json' `
  -Body $update
```

## Public endpoint

The `POST /api/customers/register` remains public for demo purposes.

## Run Postman collection with Newman

A Postman collection and environment are included under `services/customers/tests`. To run the collection headlessly with Newman (Node/npm required):

```powershell
cd services\customers\tests
npm run collection
```

## Notes
- `/api/customers/login` and `/api/customers/validate` were replaced by OAuth2 token issuance.
- Demo setup is intentionally insecure; do not use this configuration in production.
