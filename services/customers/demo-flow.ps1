# Demo script: register -> OAuth2 Authorization Code + PKCE login -> validate
# Usage: run in PowerShell from the repo root (or from this folder)
# Example: pwsh ./services/customers/demo-flow.ps1

param(
    [string]$CustomersBaseUrl = 'http://localhost:8082/api/customers',
    [string]$AuthBaseUrl = 'http://localhost:9000',
    [string]$RedirectUri = 'http://localhost:3000/callback',
    [string]$Password = 'P@ssw0rd!',
    [bool]$CopyTokenToClipboard = $true
)

function Read-ResponseBody($response) {
    try {
        if ($null -ne $response -and $response.Content) {
            return $response.Content
        }
    } catch {}
    return $null
}

function New-PkcePair {
    # return hashtable with code_verifier and code_challenge
    $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
    $rnd = New-Object System.Random
    $verifier = -join (1..64 | ForEach-Object { $chars[$rnd.Next(0,$chars.Length)] })
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $bytes = [System.Text.Encoding]::ASCII.GetBytes($verifier)
    $hash = $sha256.ComputeHash($bytes)
    $challenge = [System.Convert]::ToBase64String($hash).TrimEnd('=') -replace '\+','-' -replace '/','_'
    return @{ code_verifier = $verifier; code_challenge = $challenge }
}

# Create a timestamped username to avoid collisions
$username = "demo.user.$((Get-Date).ToString('yyyyMMddHHmmss'))"
$pwd = $Password

Write-Host "Demo flow starting against customers: $CustomersBaseUrl and auth: $AuthBaseUrl" -ForegroundColor Cyan
Write-Host "Registering user: $username" -ForegroundColor Yellow

$registerPayload = @{
    username = $username;
    password = $pwd;
    email = "$username@example.com";
    firstName = 'Demo';
    lastName = 'User';
    phone = '555-0100';
    address = '123 Demo St';
    city = 'DemoCity';
    state = 'CA';
    zipCode = '90210'
}

try {
    $regJson = $registerPayload | ConvertTo-Json
    $regResp = Invoke-RestMethod -Method Post -Uri "$CustomersBaseUrl/register" -ContentType 'application/json' -Body $regJson -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Register response:" -ForegroundColor Green
    $regResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Register failed or user exists. Attempting to continue..." -ForegroundColor Yellow
    if ($_.Exception.Response) {
        try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $body = $sr.ReadToEnd(); Write-Host "Register error body: $body" -ForegroundColor Red } catch { Write-Host "Could not read error response." -ForegroundColor Red }
    } else { Write-Host $_.Exception.Message -ForegroundColor Red }
}

# --- Begin Authorization Code + PKCE automated login ---
Write-Host "\nStarting OAuth2 Authorization Code + PKCE flow against $AuthBaseUrl" -ForegroundColor Yellow
$clientId = 'demo-spa'
$scopes = 'openid profile customers.read customers.write'
$pkce = New-PkcePair
$state = [guid]::NewGuid().Guid

$authorizeParams = @{
    response_type = 'code'
    client_id = $clientId
    scope = $scopes
    redirect_uri = $RedirectUri
    code_challenge = $pkce.code_challenge
    code_challenge_method = 'S256'
    state = $state
}

$authorizeQuery = ($authorizeParams.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Web.HttpUtility]::UrlEncode($_.Value))" }) -join '&'
$authUrl = "$AuthBaseUrl/oauth2/authorize?$authorizeQuery"

# Use a web session to follow the login flow (cookies preserved)
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

try {
    # Step 1: load the authorization endpoint (should redirect to login page or return login form)
    $initial = Invoke-WebRequest -Uri $authUrl -WebSession $session -MaximumRedirection 0 -ErrorAction Stop
    # If we got a 200, the login form likely returned in the body; if 302, it might have redirected to /login
} catch [System.Management.Automation.MethodInvocationException] {
    # Expect a 302 redirection to the login form; extract Location
    $resp = $_.Exception.InvocationInfo.Line
} catch [System.Net.WebException] {
    $we = $_.Exception
    if ($we.Response -ne $null) {
        $status = ([int]$we.Response.StatusCode)
        if ($status -eq 302 -or $status -eq 303) {
            $loc = $we.Response.GetResponseHeader('Location')
            # Follow to login
            $loginUrl = $loc
            Write-Host "Redirected to: $loginUrl" -ForegroundColor Gray
            $loginPage = Invoke-WebRequest -Uri $loginUrl -WebSession $session -ErrorAction Stop
        } else {
            throw
        }
    } else { throw }
}

# If we don't yet have loginPage, try to GET /login explicitly
if (-not $loginPage) {
    $loginPage = Invoke-WebRequest -Uri "$AuthBaseUrl/login" -WebSession $session -ErrorAction Stop
}

# Parse login form action and fields
$form = $loginPage.Forms[0]
if (-not $form) {
    Write-Host "Could not find login form on auth server; aborting." -ForegroundColor Red
    exit 1
}
$action = $form.Action
# Build form fields; override username/password
$fields = @{}
foreach ($field in $form.Fields.GetEnumerator()) { $fields[$field.Key] = $field.Value }
$fields['username'] = $username
$fields['password'] = $pwd

# POST the login form
$loginResponse = Invoke-WebRequest -Uri $action -Method Post -Body $fields -WebSession $session -MaximumRedirection 0 -ErrorAction SilentlyContinue

# After successful login, auth server should redirect to redirect_uri with code
$redirectLocation = $null
if ($loginResponse -and $loginResponse.StatusCode -in 300..399) {
    $redirectLocation = $loginResponse.Headers['Location']
} else {
    # Some versions may return a 200 with a meta-refresh or script; attempt to find Location header in response
    if ($loginResponse.Headers['Location']) { $redirectLocation = $loginResponse.Headers['Location'] }
}

if (-not $redirectLocation) {
    Write-Host "Login did not return an authorization code redirect. Response status: $($loginResponse.StatusCode)" -ForegroundColor Red
    exit 1
}

Write-Host "Received redirect: $redirectLocation" -ForegroundColor Green

# Parse code and state from redirectLocation
$uri = [System.Uri]$redirectLocation
$params = [System.Web.HttpUtility]::ParseQueryString($uri.Query)
$code = $params['code']
$returnedState = $params['state']
if ($returnedState -ne $state) { Write-Host "State mismatch (possible CSRF)" -ForegroundColor Yellow }
if (-not $code) { Write-Host "No code found in redirect; aborting." -ForegroundColor Red; exit 1 }

Write-Host "Authorization code obtained: $code" -ForegroundColor Green

# Exchange authorization code for tokens
$tokenEndpoint = "$AuthBaseUrl/oauth2/token"
$tokenBody = @{
    grant_type = 'authorization_code'
    code = $code
    redirect_uri = $RedirectUri
    client_id = $clientId
    code_verifier = $pkce.code_verifier
}

$tokenResponse = Invoke-RestMethod -Method Post -Uri $tokenEndpoint -ContentType 'application/x-www-form-urlencoded' -Body ($tokenBody.GetEnumerator() | ForEach-Object { "$($_.Key)=$([System.Web.HttpUtility]::UrlEncode($_.Value))" } -join '&') -ErrorAction Stop

Write-Host "Token response:" -ForegroundColor Green
$tokenResponse | ConvertTo-Json -Depth 5 | Write-Host

$accessToken = $tokenResponse.access_token
$idToken = $tokenResponse.id_token

if (-not $accessToken) { Write-Host "No access token returned; aborting." -ForegroundColor Red; exit 1 }

$bearer = "Bearer $accessToken"
Write-Host "\nAccess token (Bearer):" -ForegroundColor Yellow
Write-Host $bearer -ForegroundColor Green
if ($CopyTokenToClipboard) { try { Set-Clipboard -Value $bearer; Write-Host "(Bearer copied to clipboard)" -ForegroundColor Green } catch { Write-Host "Could not copy token to clipboard: $($_.Exception.Message)" -ForegroundColor Yellow } }

# Validate token by calling customers /validate if present OR call a protected endpoint
Write-Host "\nValidating token against customers service..." -ForegroundColor Yellow
$validatePayload = @{ token = $accessToken; username = $username }
try {
    $validateJson = $validatePayload | ConvertTo-Json
    $valResp = Invoke-RestMethod -Method Post -Uri "$CustomersBaseUrl/validate" -ContentType 'application/json' -Body $validateJson -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Validate response:" -ForegroundColor Green
    $valResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Validate endpoint failed or not present, calling protected customer GET as fallback..." -ForegroundColor Yellow
    try {
        $apiResp = Invoke-RestMethod -Method Get -Uri "$CustomersBaseUrl" -Headers @{ Authorization = "Bearer $accessToken" } -TimeoutSec 30 -ErrorAction Stop
        Write-Host "Customers API response:" -ForegroundColor Green
        $apiResp | ConvertTo-Json -Depth 5 | Write-Host
    } catch {
        Write-Host "Protected API call failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "\nDemo flow completed." -ForegroundColor Cyan
Write-Host "Registered username: $username" -ForegroundColor Cyan
Write-Host "You can reuse this username/password pair for additional tests." -ForegroundColor Cyan
Write-Host "Bearer token (again): $bearer" -ForegroundColor Cyan
