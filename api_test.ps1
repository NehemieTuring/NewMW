$baseUrl = "http://localhost:8080/api"
$loginData = @{ email = "root"; password = "root" } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginData -ContentType "application/json"
$token = $loginResp.token
$headers = @{ Authorization = "Bearer $token" }

function Test-Endpoint($name, $url, $method = "GET", $body = $null) {
    Write-Host "Testing $name ($method $url)..." -ForegroundColor Cyan
    try {
        $params = @{
            Uri = "$baseUrl$url"
            Method = $method
            Headers = $headers
            ContentType = "application/json"
        }
        if ($body) { $params.Body = $body }
        $resp = Invoke-RestMethod @params
        Write-Host "Success!" -ForegroundColor Green
        return $resp
    } catch {
        Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.ErrorDetails) { Write-Host "Details: $($_.ErrorDetails.Message)" }
        return $null
    }
}

Write-Host "=== STARTING SUPER ADMIN API TESTS ===" -ForegroundColor Yellow

# 1. Dashboard
Test-Endpoint "Full Dashboard" "/admin/super/dashboard"

# 2. Admins List
$admins = Test-Endpoint "Get All Admins" "/admin/super/admins"

# 3. Add Admin (Test with a new user)
$newAdminData = "name=Test&firstName=Admin&email=testadmin@example.com&username=testadmin&password=Password123&role=PRESIDENT"
# POST with RequestParam needs to be handled differently in PowerShell with application/x-www-form-urlencoded
Write-Host "Testing Add Admin (POST /admin/super/admins)..." -ForegroundColor Cyan
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/admin/super/admins?$newAdminData" -Method Post -Headers $headers
    Write-Host "Success!" -ForegroundColor Green
} catch {
    Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Password Change (Test with root self-change to keep it safe)
Write-Host "Testing Password Change (PUT /admin/super/users/password)..." -ForegroundColor Cyan
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/admin/super/users/password?email=root&newPassword=root" -Method Put -Headers $headers
    Write-Host "Success!" -ForegroundColor Green
} catch {
    Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "=== TESTS COMPLETED ===" -ForegroundColor Yellow
