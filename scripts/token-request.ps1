# Obtain a client_credentials token via client_secret_post
$body = 'grant_type=client_credentials&client_id=gateway-client&client_secret=gateway-secret&scope=customers.read'
$tokenResponse = Invoke-RestMethod -Method Post -Uri 'http://localhost:9000/oauth2/token' -ContentType 'application/x-www-form-urlencoded' -Body $body -ErrorAction Stop
Write-Host "client_secret_post token:" $tokenResponse.access_token

# Obtain a client_credentials token via client_secret_basic
$cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('gateway-client:gateway-secret'))
$headers = @{ Authorization = "Basic $cred" }
$body2 = 'grant_type=client_credentials&scope=customers.read'
$tokenResponse2 = Invoke-RestMethod -Method Post -Uri 'http://localhost:9000/oauth2/token' -ContentType 'application/x-www-form-urlencoded' -Body $body2 -Headers $headers -ErrorAction Stop
Write-Host "client_secret_basic token:" $tokenResponse2.access_token

# Example: call the gateway (adjust gateway URL/port as needed)
$accessToken = $tokenResponse2.access_token
$apiHeaders = @{ Authorization = "Bearer $accessToken" }
try {
    $customers = Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/customers' -Headers $apiHeaders -ErrorAction Stop
    Write-Host "Customers response:"; $customers
} catch {
    Write-Host "Failed to call customers API through gateway: $_"
}

# Example: exchange Authorization Code (manual PKCE flow not implemented here)
Write-Host "Notes: For SPA login use Authorization Code + PKCE. This script only demonstrates client_credentials flows."
