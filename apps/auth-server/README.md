# Auth Server (Demo)

This module provides a deliberately insecure OAuth2 Authorization Server for local demos.

## Default clients
- `gateway-client` + `gateway-secret` (`client_credentials`)
- `demo-spa` (`authorization_code` + PKCE)

## Default users
- `user` / `password`
- `admin` / `password`

## Try it (client credentials)

```powershell
$token = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:9000/oauth2/token' `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body 'grant_type=client_credentials&client_id=gateway-client&client_secret=gateway-secret&scope=customers.read'

$token.access_token
```

