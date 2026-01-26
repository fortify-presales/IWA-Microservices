# Demo script: register -> login -> validate
# Usage: run in PowerShell from the repo root (or from this folder)
# Example: pwsh ./services/customers/demo-flow.ps1

param(
    [string]$BaseUrl = 'http://localhost:8082/api/customers',
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

# Create a timestamped username to avoid collisions
$username = "demo.user.$((Get-Date).ToString('yyyyMMddHHmmss'))"
$pwd = $Password

Write-Host "Demo flow starting against $BaseUrl" -ForegroundColor Cyan
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
    $regResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/register" -ContentType 'application/json' -Body $regJson -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Register response:" -ForegroundColor Green
    $regResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Register failed or user exists. Attempting to continue..." -ForegroundColor Yellow
    # Try to show response body if available
    if ($_.Exception.Response) {
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $body = $sr.ReadToEnd()
            Write-Host "Register error body: $body" -ForegroundColor Red
        } catch {
            Write-Host "Could not read error response." -ForegroundColor Red
        }
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

# Login
Write-Host "\nLogging in as: $username" -ForegroundColor Yellow
$loginPayload = @{ username = $username; password = $pwd }
try {
    $loginJson = $loginPayload | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/login" -ContentType 'application/json' -Body $loginJson -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Login successful. Response:" -ForegroundColor Green
    $loginResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Login failed:" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $body = $sr.ReadToEnd()
            Write-Host "Login error body: $body" -ForegroundColor Red
        } catch {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    exit 1
}

$token = $null
if ($loginResp -is [System.Collections.IDictionary] -and $loginResp.ContainsKey('token')) {
    $token = $loginResp.token
} elseif ($loginResp.token) {
    $token = $loginResp.token
}

if (-not $token) {
    Write-Host "No token returned from login; aborting." -ForegroundColor Red
    exit 1
}

# Prepare Bearer value and optionally copy to clipboard
$bearer = "Bearer $token"
Write-Host "\nBearer token (copy this into Swagger Authorize dialog):" -ForegroundColor Yellow
Write-Host $bearer -ForegroundColor Green

if ($CopyTokenToClipboard) {
    try {
        # PowerShell's Set-Clipboard is available on Windows PowerShell 5+ and PowerShell Core.
        Set-Clipboard -Value $bearer
        Write-Host "(Bearer value copied to clipboard)" -ForegroundColor Green
    } catch {
        Write-Host "Could not copy token to clipboard: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "\nValidating token..." -ForegroundColor Yellow
$validatePayload = @{ token = $token; username = $username }
try {
    $validateJson = $validatePayload | ConvertTo-Json
    $valResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/validate" -ContentType 'application/json' -Body $validateJson -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Validate response:" -ForegroundColor Green
    $valResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Validation failed:" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $body = $sr.ReadToEnd()
            Write-Host "Validate error body: $body" -ForegroundColor Red
        } catch {
            Write-Host $_.Exception.Message -ForegroundColor Red
        }
    } else {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    exit 1
}

Write-Host "\nDemo flow completed." -ForegroundColor Cyan
Write-Host "Registered username: $username" -ForegroundColor Cyan
Write-Host "You can reuse this username/password pair for additional tests." -ForegroundColor Cyan
Write-Host "Bearer token (again): $bearer" -ForegroundColor Cyan
