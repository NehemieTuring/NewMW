$loginUrl = "http://localhost:8080/api/auth/login"
$body = @{
    email = "root"
    password = "root"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $body -ContentType "application/json"
$response | ConvertTo-Json | Out-File "login_test.json"
