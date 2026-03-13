# OAuth2 Authentication Server

This module provides a deliberately insecure OAuth2 Authorization Server for security testing
and demos.

## Default clients
- `gateway-client` + `gateway-secret` (`client_credentials`)
- `demo-spa` (`authorization_code` + PKCE)

## Default users
- `user1` / `password`
- `user2` / `password`
- `admin` / `password`

## Try it (client credentials)

```powershell
$token = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:9000/oauth2/token' `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body 'grant_type=client_credentials&client_id=gateway-client&client_secret=gateway-secret&scope=customers.read'

$token.access_token
```

# Auth Server (Configuration)

This auth server is a demo Authorization Server used by the project. For deployment in different environments (dev/staging/prod) you must configure the public issuer URL and the redirect/post-logout URIs for registered clients.

Environment variables and properties

- `AUTH_ISSUER_URI` or `auth.issuer-uri` — The public issuer base URL for the auth server (e.g., `https://auth.staging.example.com`). This value is used in tokens and discovery (`/.well-known/openid-configuration`).

- `DEMO_SPA_REDIRECT_URIS` or `demo-spa.redirect-uris` — Comma-separated list of redirect URIs for the `demo-spa` client. Example:

  `DEMO_SPA_REDIRECT_URIS=https://app.staging.example.com/callback,https://app.prod.example.com/callback`

- `DEMO_SPA_POSTLOGOUT_REDIRECT_URIS` or `demo-spa.postlogout-redirect-uris` — Comma-separated list of post-logout redirect URIs for `demo-spa`.

How it works

The server reads these properties at startup and registers the `demo-spa` client with the provided redirect and post-logout URIs. This avoids recompiling code when deploying to different domains.

Make sure the public-facing redirect URI is reachable by user browsers and matches exactly the value configured here (including scheme and trailing slash).
