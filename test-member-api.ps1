# D:\MutuelleNehemie\CODE\mutuelle-backend\test-member-api.ps1
$BASE_URL = "http://localhost:8080/api"
$AUTH_URL = "$BASE_URL/auth/login"
$CREDENTIALS = @{ identifier = "calvin@gmail.com"; password = "calvin@123" }

function Show-Res($task, $success, $detail = "") {
    $col = if($success) { "Green" } else { "Red" }
    $sym = if($success) { "[OK]" } else { "[ECHEC]" }
    Write-Host ("{0,-40} {1} {2}" -f $task, $sym, $detail) -ForegroundColor $col
}

Write-Host "`n" + ("=" * 70) -ForegroundColor Cyan
Write-Host "   TEST DE CONFORMITÉ TOTAL - PORTAIL MEMBRE (Calvin)" -ForegroundColor Cyan
Write-Host ("=" * 70) + "`n" -ForegroundColor Cyan

# 1. AUTHENTIFICATION
try {
    $authRes = Invoke-RestMethod -Uri $AUTH_URL -Method Post -Body ($CREDENTIALS | ConvertTo-Json) -ContentType "application/json"
    $TOKEN = $authRes.token
    $HEADERS = @{ "Authorization" = "Bearer $TOKEN"; "Content-Type" = "application/json" }
    Show-Res "[1] Authentification" $true "Token obtenu"
} catch { Show-Res "[1] Authentification" $false $_.Exception.Message; return }

# 2. PROFIL (GET & PUT)
try {
    $profile = Invoke-RestMethod -Uri "$BASE_URL/member/profile" -Headers $HEADERS
    Show-Res "[2.1] Lecture Profil" $true "$($profile.firstName) $($profile.name)"
    # Test Update (on change juste le prénom temporairement)
    $upBody = @{ name=$profile.name; firstName=$profile.firstName; username=$profile.username; tel=$profile.tel; address="Test Address" }
    $updated = Invoke-RestMethod -Uri "$BASE_URL/member/profile?name=$($profile.name)&firstName=$($profile.firstName)&username=$($profile.username)&tel=$($profile.tel)&address=$([uri]::EscapeDataString('Test Address'))" -Method Put -Headers $HEADERS
    Show-Res "[2.2] Update Profil (Adresse)" $true
} catch { Show-Res "[2] Profil" $false $_.Exception.Message }

# 3. FINANCES
try {
    $status = Invoke-RestMethod -Uri "$BASE_URL/member/status" -Headers $HEADERS
    $debts = Invoke-RestMethod -Uri "$BASE_URL/member/debts" -Headers $HEADERS
    $payments = Invoke-RestMethod -Uri "$BASE_URL/member/payments" -Headers $HEADERS
    Show-Res "[3.1] Statut Membre" $true $status
    Show-Res "[3.2] Liste Dettes" $true "$($debts.Count) ligne(s)"
    Show-Res "[3.3] Historique Paiements" $true "$($payments.Count) ligne(s)"
} catch { Show-Res "[3] Finances" $false $_.Exception.Message }

# 4. ÉPARGNE & EMPRUNTS
try {
    $bal = Invoke-RestMethod -Uri "$BASE_URL/member/savings/balance" -Headers $HEADERS
    $savings = Invoke-RestMethod -Uri "$BASE_URL/member/savings" -Headers $HEADERS
    $loans = Invoke-RestMethod -Uri "$BASE_URL/member/borrowings" -Headers $HEADERS
    Show-Res "[4.1] Solde Épargne" $true "$bal Frs"
    Show-Res "[4.2] Liste Emprunts" $true "$($loans.Count) prêt(s)"
} catch { Show-Res "[4] Banque" $false $_.Exception.Message }

# 5. AIDES SOCIALES
try {
    $types = Invoke-RestMethod -Uri "$BASE_URL/member/helps/types" -Headers $HEADERS
    $active = Invoke-RestMethod -Uri "$BASE_URL/member/helps/active" -Headers $HEADERS
    Show-Res "[5.1] Types d'Aides" $true "$($types.Count) types"
    Show-Res "[5.2] Collectes en cours" $true "$($active.Count) aides"
} catch { Show-Res "[5] Aides" $false $_.Exception.Message }

# 6. MESSAGERIE (CHAT COMPLET)
try {
    # 6.1 Envoi
    $msg = Invoke-RestMethod -Uri "$BASE_URL/member/chat/send?content=Message+de+test+complet" -Method Post -Headers $HEADERS
    $msgId = $msg.id
    Show-Res "[6.1] Envoi Message" ($msg.sender.email -eq "calvin@gmail.com") "ID: $msgId"

    # 6.2 Modification
    $edited = Invoke-RestMethod -Uri "$BASE_URL/member/chat/messages/$msgId?content=Message+Modifié+Auto" -Method Put -Headers $HEADERS
    Show-Res "[6.2] Modification Message" $edited.edited

    # 6.3 Recherche
    $search = Invoke-RestMethod -Uri "$BASE_URL/member/chat/group/search?query=Modifié" -Headers $HEADERS
    Show-Res "[6.3] Recherche (Groupe)" ($search.Count -gt 0) "Trouvé"

    # 6.4 Non lus
    $unread = Invoke-RestMethod -Uri "$BASE_URL/member/chat/unread" -Headers $HEADERS
    Show-Res "[6.4] Messages non lus" $true "$unread"

    # 6.5 Suppression
    Invoke-RestMethod -Uri "$BASE_URL/member/chat/messages/$msgId" -Method Delete -Headers $HEADERS
    Show-Res "[6.5] Suppression Message" $true
} catch { Show-Res "[6] Messagerie" $false $_.Exception.Message }

# 7. VIE MUTUELLE
try {
    $sessions = Invoke-RestMethod -Uri "$BASE_URL/member/sessions" -Headers $HEADERS
    $exercises = Invoke-RestMethod -Uri "$BASE_URL/member/exercises" -Headers $HEADERS
    Show-Res "[7.1] Liste Sessions" $true "$($sessions.Count)"
    Show-Res "[7.2] Liste Exercices" $true "$($exercises.Count)"
} catch { Show-Res "[7] Vie Mutuelle" $false $_.Exception.Message }

Write-Host "`n" + ("=" * 70) -ForegroundColor Cyan
Write-Host "   FIN DU TEST TOTAL" -ForegroundColor Cyan
Write-Host ("=" * 70) + "`n" -ForegroundColor Cyan
"C:\Users\bbatc\OneDrive\Images\Captures d’écran\Capture d'écran 2026-04-21 013004.png"