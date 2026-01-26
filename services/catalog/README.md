# Catalog Service - Demo flow and API key usage

This directory contains a small PowerShell demo script (`demo-flow.ps1`) that exercises the Catalog service APIs. The catalog service exposes public GET endpoints (list, search, category, get-by-id) and secured write endpoints (POST/PUT/DELETE) protected by a simple API key header for demo purposes.

> WARNING: This project intentionally contains security weaknesses for training/demo purposes (hard-coded demo API key, SQL injection vulnerabilities, permissive CORS). Do NOT use these patterns in production.

## Demo script

`demo-flow.ps1` performs the following steps:

1. Lists products (GET /api/products) — public
2. Searches products (GET /api/products/search?q=...) — public
3. Lists by category (GET /api/products/category/{category}) — public
4. Creates a product (POST /api/products) — secured via `X-API-KEY` header
5. Updates the created product (PUT /api/products/{id}) — secured via `X-API-KEY` header
6. Deletes the created product (DELETE /api/products/{id}) — secured via `X-API-KEY` header

The script will by default use:
- Base URL: `http://localhost:8081/api/products`
- Demo API key: `demo-secret-key`

Usage examples

Run the demo flow from the repository root (PowerShell):

```powershell
pwsh ./services/catalog/demo-flow.ps1
# or
./services/catalog/demo-flow.ps1
```

You can override default parameters:

```powershell
pwsh ./services/catalog/demo-flow.ps1 -BaseUrl 'http://localhost:8081/api/products' -ApiKey 'demo-secret-key'
```

Manual curl examples

- List products (public):

```bash
curl -i http://localhost:8081/api/products
```

- Create product (secured):

```bash
curl -i -X POST -H "Content-Type: application/json" -H "X-API-KEY: demo-secret-key" -d '{"name":"Demo","description":"d","category":"Demo","price":1.0,"stockQuantity":10}' http://localhost:8081/api/products
```

## Postman collection

A Postman collection and environment for the Catalog service are provided under `services/catalog/postman/`:

- `CatalogService.postman_collection.json` — Postman collection with requests for all endpoints (List, Get by ID, Search, Category, Create, Update, Delete)
- `CatalogService.postman_environment.json` — Environment with useful variables: `baseUrl`, `apiKey`, `productId`, `searchQuery`, `category`

Import and run in Postman UI

1. Open Postman and choose Import -> File.
2. Select `CatalogService.postman_collection.json` and `CatalogService.postman_environment.json` and import them.
3. Select the imported environment (top right) so `{{baseUrl}}` and `{{apiKey}}` resolve.
4. Run individual requests or use the Collection Runner to run through them in sequence.

Run the collection with Newman (CLI)

1. Install newman if you don't have it:

```bash
npm install -g newman
```

2. Run the collection with the environment:

```bash
newman run services/catalog/postman/CatalogService.postman_collection.json -e services/catalog/postman/CatalogService.postman_environment.json
```

Notes

- The `Create` request in the collection creates a new product and the response body contains the created `id`. Update the `productId` environment variable in Postman if you want subsequent Update/Delete requests to target that created product.
- The collection uses `{{apiKey}}` for secured requests. The default value is `demo-secret-key`.

## Structured 401 error responses (API key info)

When a secured endpoint is called without a valid API key, the service returns a structured JSON error (HTTP 401) with details about the missing/invalid header and remediation guidance. This is documented in the OpenAPI/Swagger for the secured endpoints.

Example 401 response body (application/json):

```json
{
  "code": "API_KEY_MISSING",
  "message": "Missing or invalid API key",
  "requiredHeader": "X-API-KEY",
  "details": "Provide the X-API-KEY header with a valid API key. Contact the API owner to obtain a key.",
  "timestamp": "2026-01-26T16:02:37.123"
}
```

Notes and recommendations

- The demo API key is hard-coded for convenience in this training/demo repository. For real apps, keys should be stored and validated securely, rotated, and scoped.
- The repository contains intentional SQL injection vulnerabilities to demonstrate security testing techniques — do not copy these patterns.
- If the demo app fails to start due to a port conflict, stop the conflicting process or run the service on another port.

If you want, I can:
- Make the demo script work on a different default port (if your local instance runs on a different port), or
- Add a README section with troubleshooting steps and expected outputs.
