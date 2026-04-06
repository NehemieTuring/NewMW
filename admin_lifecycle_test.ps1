$baseUrl = "http://localhost:8080/api"
$loginData = @{ email = "root"; password = "root" } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginData -ContentType "application/json"
$token = $loginResp.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "--- TEST: Create -> Get -> Delete sequence ---" -ForegroundColor Cyan

$username = "temp_admin_" + (Get-Random)
$query = "name=Temp&firstName=Admin&email=$username@example.com&username=$username&password=Pass123&role=TRESORIER"

# 1. Create
try {
    $admin = Invoke-RestMethod -Uri "$baseUrl/admin/super/admins?$query" -Method Post -Headers $headers
    $id = $admin.id
    Write-Host "Created admin with ID: $id" -ForegroundColor Green
    
    # 2. Get
    $got = Invoke-RestMethod -Uri "$baseUrl/admin/super/admins/$id" -Method Get -Headers $headers
    Write-Host "Successfully fetched admin $id profile." -ForegroundColor Green
    
    # 3. Deactivate
    Invoke-RestMethod -Uri "$baseUrl/admin/super/admins/$id/deactivate" -Method Put -Headers $headers
    Write-Host "Successfully deactivated admin $id." -ForegroundColor Green
    
    # 4. Delete
    Invoke-RestMethod -Uri "$baseUrl/admin/super/admins/$id" -Method Delete -Headers $headers
    Write-Host "Successfully deleted admin $id." -ForegroundColor Green
} catch {
    Write-Host "SEQUENCE FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "--- END SEQUENCE TEST ---" -ForegroundColor Cyan
