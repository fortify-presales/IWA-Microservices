# Demo script for Catalog service: exercise public and secured endpoints
# Usage: run in PowerShell from the repo root (or from this folder)
# Example: pwsh ./services/catalog/demo-flow.ps1

param(
    [string]$BaseUrl = 'http://localhost:8081/api/products',
    [string]$ApiKey = 'demo-secret-key',
    [bool]$CopyCreatedIdToClipboard = $true
)

function Read-ResponseBody($response) {
    try {
        if ($null -ne $response -and $response.Content) {
            return $response.Content
        }
    } catch {}
    return $null
}

Write-Host "Catalog demo flow starting against $BaseUrl" -ForegroundColor Cyan
Write-Host "Using API Key (for secured operations): $ApiKey" -ForegroundColor Yellow

# 1) List products (public GET)
Write-Host "\n1) Listing products (public GET)" -ForegroundColor Yellow
try {
    $listResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl" -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Products:" -ForegroundColor Green
    $listResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Failed to list products:" -ForegroundColor Red
    if ($_.Exception.Response) { try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $body = $sr.ReadToEnd(); Write-Host $body -ForegroundColor Red } catch { Write-Host $_.Exception.Message -ForegroundColor Red } } else { Write-Host $_.Exception.Message -ForegroundColor Red }
}

# 2) Search (public GET)
Write-Host "\n2) Search products (public GET)" -ForegroundColor Yellow
try {
    $searchResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/search?q=aspirin" -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Search results:" -ForegroundColor Green
    $searchResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Search failed:" -ForegroundColor Red
}

# 3) List by category (public GET)
Write-Host "\n3) List by category 'Pain Relief' (public GET)" -ForegroundColor Yellow
try {
    $catResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/category/Pain%20Relief" -TimeoutSec 30 -ErrorAction Stop
    Write-Host "Category results:" -ForegroundColor Green
    $catResp | ConvertTo-Json -Depth 5 | Write-Host
} catch {
    Write-Host "Category query failed:" -ForegroundColor Red
}

# 4) Create a new product (SECURED - requires X-API-KEY)
Write-Host "\n4) Creating a new product (SECURED - POST)" -ForegroundColor Yellow
$createPayload = @{
    name = "Demo Product $((Get-Date).ToString('yyyyMMddHHmmss'))";
    description = "Demo product created by demo-flow script";
    category = "Demo";
    price = 4.99;
    stockQuantity = 42;
    imageUrl = "http://example.com/demo.png";
    requiresPrescription = $false;
    manufacturer = "DemoCorp"
}

$createdId = $null
try {
    $json = $createPayload | ConvertTo-Json
    $createResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl" -ContentType 'application/json' -Body $json -TimeoutSec 30 -Headers @{ 'X-API-KEY' = $ApiKey } -ErrorAction Stop
    Write-Host "Create response (body):" -ForegroundColor Green
    $createResp | ConvertTo-Json -Depth 5 | Write-Host
    if ($createResp -is [System.Collections.IDictionary] -and $createResp.ContainsKey('id')) { $createdId = $createResp.id }
    elseif ($createResp.id) { $createdId = $createResp.id }
} catch {
    Write-Host "Create failed:" -ForegroundColor Red
    if ($_.Exception.Response) { try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $body = $sr.ReadToEnd(); Write-Host $body -ForegroundColor Red } catch { Write-Host $_.Exception.Message -ForegroundColor Red } } else { Write-Host $_.Exception.Message -ForegroundColor Red }
}

if ($createdId) {
    Write-Host "Created product id: $createdId" -ForegroundColor Cyan
    if ($CopyCreatedIdToClipboard) {
        try { Set-Clipboard -Value $createdId; Write-Host "(Created id copied to clipboard)" -ForegroundColor Green } catch { Write-Host "Could not copy to clipboard: $($_.Exception.Message)" -ForegroundColor Yellow }
    }
} else {
    Write-Host "No created id returned." -ForegroundColor Yellow
}

# 5) Update the created product (SECURED - PUT)
if ($createdId) {
    Write-Host "\n5) Updating the created product (SECURED - PUT)" -ForegroundColor Yellow
    $updatePayload = @{
        name = "Updated Demo Product";
        description = "Updated by demo-flow script";
        category = "Demo";
        price = 5.99;
        stockQuantity = 30;
        imageUrl = "http://example.com/demo-updated.png";
        requiresPrescription = $false;
        manufacturer = "DemoCorp"
    }
    try {
        $json = $updatePayload | ConvertTo-Json
        $updateResp = Invoke-RestMethod -Method Put -Uri "$BaseUrl/$createdId" -ContentType 'application/json' -Body $json -TimeoutSec 30 -Headers @{ 'X-API-KEY' = $ApiKey } -ErrorAction Stop
        Write-Host "Update response:" -ForegroundColor Green
        $updateResp | ConvertTo-Json -Depth 5 | Write-Host
    } catch {
        Write-Host "Update failed:" -ForegroundColor Red
        if ($_.Exception.Response) { try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $body = $sr.ReadToEnd(); Write-Host $body -ForegroundColor Red } catch { Write-Host $_.Exception.Message -ForegroundColor Red } } else { Write-Host $_.Exception.Message -ForegroundColor Red }
    }
}

# 6) Delete the created product (SECURED - DELETE)
if ($createdId) {
    Write-Host "\n6) Deleting the created product (SECURED - DELETE)" -ForegroundColor Yellow
    try {
        $delResp = Invoke-RestMethod -Method Delete -Uri "$BaseUrl/$createdId" -TimeoutSec 30 -Headers @{ 'X-API-KEY' = $ApiKey } -ErrorAction Stop
        Write-Host "Delete succeeded (server returned body):" -ForegroundColor Green
        $delResp | ConvertTo-Json -Depth 5 | Write-Host
    } catch {
        # Many APIs return 204 No Content on delete; Invoke-RestMethod throws on 204 with no content, so we catch and detect status code via the exception
        if ($_.Exception.Response) {
            try {
                $status = $_.Exception.Response.StatusCode.value__
                Write-Host "Delete response status: $status" -ForegroundColor Green
            } catch { Write-Host "Delete completed (no content)" -ForegroundColor Green }
        } else {
            Write-Host "Delete failed: $_.Exception.Message" -ForegroundColor Red
        }
    }
}

Write-Host "\nDemo flow completed." -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl" -ForegroundColor Cyan
Write-Host "API Key used for secured ops: $ApiKey" -ForegroundColor Cyan


