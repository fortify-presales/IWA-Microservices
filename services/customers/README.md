# Customers Service

## Demo quick-start (PowerShell)

This page provides copy-paste PowerShell commands to run the Customers service locally and exercise the login/register/validate endpoints during a demo.

## Prerequisites
- Java 17
- Gradle wrapper included in the repo (use ./gradlew or .\gradlew on Windows)
- PowerShell (Windows) or PowerShell Core

## Start the Customers service

```powershell
# From the repository root
.\gradlew :services:customers:bootRun
```

Wait until logs show "Started CustomersServiceApplication" and Tomcat is listening on port 8082.

## Swagger UI

- Open in your browser: http://localhost:8082/swagger-ui.html or http://localhost:8082/swagger-ui/index.html
- Actuator health: http://localhost:8082/actuator/health

## Login (PowerShell recommended)

Use PowerShell objects and ConvertTo-Json to avoid quoting/encoding issues:

```powershell
$body = @{ username = 'john.doe'; password = 'password123' }
$json = $body | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/customers/login' -ContentType 'application/json' -Body $json
```

If successful you will get a JSON object with the token, e.g.:

```json
{ "token": "<jwt>", "customerId": 1, "username": "john.doe" }
```

## Capture token and validate

```powershell
# Capture token from login response
$resp = Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/customers/login' -ContentType 'application/json' -Body ( @{ username='john.doe'; password='password123' } | ConvertTo-Json )
$token = $resp.token

# Validate token
$val = @{ token = $token; username = 'john.doe' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/customers/validate' -ContentType 'application/json' -Body $val
```

## Register a new user

```powershell
$reg = @{
  username = 'new.user'
  password = 'P@ssw0rd!'
  email = 'new.user@example.com'
  firstName = 'New'
  lastName = 'User'
  phone = '555-0100'
  address = '123 Demo St'
  city = 'DemoCity'
  state = 'CA'
  zipCode = '90210'
}
Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/api/customers/register' -ContentType 'application/json' -Body ( $reg | ConvertTo-Json )
```

## Alternative: curl on Windows

PowerShell aliases `curl` to Invoke-WebRequest. If you have native curl available, call `curl.exe` explicitly and pass a UTF-8 file to avoid quoting issues:

```powershell
@'
{"username":"john.doe","password":"password123"}
'@ > temp_login.json
# Ensure UTF-8 without BOM
Set-Content -Path temp_login.json -Value (Get-Content temp_login.json -Raw) -Encoding UTF8

curl.exe -v -X POST "http://localhost:8082/api/customers/login" -H "Content-Type: application/json" --data-binary @temp_login.json
```

## Troubleshooting

- 400 with "must not be blank": service saw empty fields — ensure your request body is valid JSON and Content-Type is application/json. Using PowerShell's ConvertTo-Json avoids most quoting issues.
- JSON parse errors with backslashes: usually quoting/encoding problems; write JSON to a UTF-8 file and post the file (see curl example).
- If you change code, rebuild then restart the service:

```powershell
.\gradlew :services:customers:build --no-daemon
.\gradlew :services:customers:bootRun
```

## Notes

- The demo application includes intentional vulnerabilities (plain-text passwords, permissive CORS); treat this environment as demo-only and avoid using real credentials.
- Swagger examples are provided for login/register/validate operations to help demo participants.
- Using the token in Swagger UI
  - After you obtain a token (via the Login operation or the demo script), open the Swagger UI at http://localhost:8082/swagger-ui.html.
  - Click the "Authorize" button (top-right). In the value field paste the exact string below (including the word "Bearer" and a space):

```
Bearer <your-jwt-token-here>
```

Example flow to get and copy the token to clipboard (PowerShell):

```powershell
# Run the demo flow which registers/logs in a user and copies the Bearer token to the clipboard
pwsh .\services\customers\demo-flow.ps1 -CopyTokenToClipboard $true

# The script will print the Bearer value and copy it. Paste the copied value into the Swagger Authorize dialog.
```

## Run Postman collection (Newman)

You can run the provided Postman collection locally (interactive in Postman) or from the terminal using Newman (CLI runner for Postman).

Prerequisites
- Node.js and npm (or npx available). Install from https://nodejs.org/ if you don't have them.
- Ensure the Customers service is running on http://localhost:8082 (see Start the Customers service above).

Files provided
- Collection: `services/customers/postman/CustomersService.postman_collection.json`
- Environment: `services/customers/postman/CustomersService.postman_environment.json` (defaults to `username = john.doe` / `password = password123`)
- Newman helper/npm script: `services/customers/postman/package.json` (script: `npm run run-collection`)

Run in Postman (GUI)
1. Import the collection: File → Import → choose `CustomersService.postman_collection.json`.
2. Import the environment: File → Import → choose `CustomersService.postman_environment.json`.
3. Select the imported environment in the top-right environment selector.
4. Run `01 - Login (get token)` to populate `token` (Bearer ...) and `rawToken` (raw JWT) environment variables.
5. Run subsequent requests in order (or use the Collection Runner).

Run from the terminal with Newman (quick, no install required)

Open a PowerShell prompt and run:

```powershell
cd services\customers\postman
npx newman run CustomersService.postman_collection.json -e CustomersService.postman_environment.json --delay-request 50 --reporters cli
```

Or install dev dependencies and run the npm script:

```powershell
cd services\customers\postman
npm install
npm run run-collection
```

CI / reports
- To create a JSON report for CI, add reporters to the Newman command, for example:

```powershell
npx newman run CustomersService.postman_collection.json -e CustomersService.postman_environment.json --reporters cli,json --reporter-json-export report.json
```

Notes
- The collection stores two token variables after login:
  - `token` — the string `Bearer <jwt>` (use this in Authorization headers)
  - `rawToken` — the compact JWT without the `Bearer ` prefix (used in the `validate` request body)
- If the validate request contains the `Bearer ` prefix in the body it will fail with a base64 decoding error (Illegal base64url character: ' '). The collection sends `rawToken` to avoid this.
- Change `baseUrl`, `username`, or `password` in the environment file if you want to target a different host or user.
