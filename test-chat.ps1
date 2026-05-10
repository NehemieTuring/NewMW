##############################################
# Script de test des endpoints Chat/Messagerie
# Compte: Rose (nerose@gmail.com / nesli@123)
##############################################

$BASE = "http://localhost:8080/api"
$sep = "`n" + ("=" * 60)

function Show-Error($err) {
    Write-Host "[ECHEC] $($err.Exception.Message)" -ForegroundColor Red
    try {
        $stream = $err.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "  Detail: $($reader.ReadToEnd())" -ForegroundColor Yellow
    } catch {}
}

# =============================================
# TEST 1 : Connexion Rose
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 1 : Connexion (Rose / nesli@123)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

$loginBody = '{"identifier":"nerose@gmail.com","password":"nesli@123"}'
try {
    $loginResp = Invoke-RestMethod -Uri "$BASE/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
    Write-Host "[OK] id=$($loginResp.id)  role=$($loginResp.role)  email=$($loginResp.email)" -ForegroundColor Green
    $TOKEN = $loginResp.token
    $USER_ID = $loginResp.id
} catch {
    Show-Error $_
    exit 1
}

$headers = @{ Authorization = "Bearer $TOKEN" }

# =============================================
# TEST 2 : Conversations existantes
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 2 : Conversations (GET /member/chat/conversations)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $convs = Invoke-RestMethod -Uri "$BASE/member/chat/conversations" -Headers $headers
    if ($convs -is [array]) {
        Write-Host "[OK] $($convs.Count) conversation(s)" -ForegroundColor Green
        $convs | ForEach-Object { Write-Host "  -> id=$($_.id) $($_.firstName) $($_.name)" }
    } else {
        Write-Host "[OK] Reponse recue" -ForegroundColor Green
    }
} catch {
    Show-Error $_
}

# =============================================
# TEST 3 : Envoi message GROUPE (sans receiverId)
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 3 : Message GROUPE (POST /member/chat/send)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $msg1 = Invoke-RestMethod -Uri "$BASE/member/chat/send?content=Bonjour+a+tous+les+membres!" -Method Post -Headers $headers
    Write-Host "[OK] ID=$($msg1.id), sender=$($msg1.sender.firstName)" -ForegroundColor Green
    Write-Host "  receiver=$(if($msg1.receiver) { $msg1.receiver.id } else { 'NULL (groupe)' })"
    Write-Host "  delivered=$($msg1.delivered), message='$($msg1.message)'"
    $MSG1_ID = $msg1.id
} catch {
    Show-Error $_
    $MSG1_ID = $null
}

# =============================================
# TEST 4 : 2eme message groupe
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 4 : 2eme message groupe" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $msg2 = Invoke-RestMethod -Uri "$BASE/member/chat/send?content=Test+pagination+et+recherche" -Method Post -Headers $headers
    Write-Host "[OK] ID=$($msg2.id)" -ForegroundColor Green
    $MSG2_ID = $msg2.id
} catch {
    Show-Error $_
    $MSG2_ID = $null
}

# =============================================
# TEST 5 : Historique GROUPE pagine
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 5 : Historique GROUPE (GET /member/chat/group/messages?page=0&size=5)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $hist = Invoke-RestMethod -Uri "$BASE/member/chat/group/messages?page=0&size=5" -Headers $headers
    Write-Host "[OK] Page $($hist.number+1)/$($hist.totalPages), total=$($hist.totalElements)" -ForegroundColor Green
    if ($hist.content) {
        $hist.content | ForEach-Object {
            $extra = ""
            if ($_.edited) { $extra += " [MODIFIE]" }
            if ($_.attachmentUrl) { $extra += " [FICHIER]" }
            Write-Host "  -> [$($_.id)] $($_.sender.firstName): $($_.message)$extra"
        }
    }
} catch {
    Show-Error $_
}

# =============================================
# TEST 6 : Recherche dans le groupe
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 6 : Recherche (GET /member/chat/group/search?query=pagination)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $search = Invoke-RestMethod -Uri "$BASE/member/chat/group/search?query=pagination" -Headers $headers
    if ($search -is [array]) {
        Write-Host "[OK] $($search.Count) resultat(s)" -ForegroundColor Green
        $search | ForEach-Object { Write-Host "  -> [$($_.id)] $($_.message)" }
    } else {
        Write-Host "[OK] Resultat recu" -ForegroundColor Green
    }
} catch {
    Show-Error $_
}

# =============================================
# TEST 7 : Edition d'un message
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 7 : Edition (PUT /member/chat/messages/$MSG1_ID)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

if ($MSG1_ID) {
    try {
        $editUri = "$BASE/member/chat/messages/${MSG1_ID}?content=Message+MODIFIE!"
        $edited = Invoke-RestMethod -Uri $editUri -Method Put -Headers $headers
        Write-Host "[OK] edited=$($edited.edited), message='$($edited.message)'" -ForegroundColor Green
    } catch {
        Show-Error $_
    }
} else {
    Write-Host "[IGNORE] Pas de message a editer" -ForegroundColor Yellow
}

# =============================================
# TEST 8 : Upload de fichier
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 8 : Upload fichier (POST /member/chat/upload)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

$testFile = Join-Path $PSScriptRoot "test_chat_file.txt"
"Fichier test mutuelle" | Out-File -FilePath $testFile -Encoding UTF8

try {
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $client.DefaultRequestHeaders.Add("Authorization", "Bearer $TOKEN")
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $fileBytes = [System.IO.File]::ReadAllBytes($testFile)
    $fileContent = New-Object System.Net.Http.ByteArrayContent(,[byte[]]$fileBytes)
    $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("text/plain")
    $multipart.Add($fileContent, "file", "test_chat_file.txt")
    $resp = $client.PostAsync("$BASE/member/chat/upload", $multipart).Result
    $body = $resp.Content.ReadAsStringAsync().Result
    if ($resp.IsSuccessStatusCode) {
        Write-Host "[OK] $body" -ForegroundColor Green
        $uploadData = $body | ConvertFrom-Json
        $ATTACH_URL = $uploadData.url
    } else {
        Write-Host "[ECHEC] Status=$($resp.StatusCode), Body=$body" -ForegroundColor Red
        $ATTACH_URL = $null
    }
    $client.Dispose()
} catch {
    Write-Host "[ECHEC] $($_.Exception.Message)" -ForegroundColor Red
    $ATTACH_URL = $null
}

# =============================================
# TEST 9 : Message avec piece jointe
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 9 : Message avec piece jointe" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

if ($ATTACH_URL) {
    try {
        $encUrl = [uri]::EscapeDataString($ATTACH_URL)
        $msgFile = Invoke-RestMethod -Uri "$BASE/member/chat/send?content=Document+important&attachmentUrl=$encUrl&attachmentType=text/plain" -Method Post -Headers $headers
        Write-Host "[OK] ID=$($msgFile.id), attachment=$($msgFile.attachmentUrl)" -ForegroundColor Green
    } catch {
        Show-Error $_
    }
} else {
    Write-Host "[IGNORE] Upload echoue" -ForegroundColor Yellow
}

# =============================================
# TEST 10 : Messages non lus
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 10 : Non lus (GET /member/chat/unread)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $unread = Invoke-RestMethod -Uri "$BASE/member/chat/unread" -Headers $headers
    Write-Host "[OK] $unread message(s) non lu(s)" -ForegroundColor Green
} catch {
    Show-Error $_
}

# =============================================
# TEST 11 : Suppression
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 11 : Suppression (DELETE /member/chat/messages/$MSG2_ID)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

if ($MSG2_ID) {
    try {
        Invoke-RestMethod -Uri "$BASE/member/chat/messages/$MSG2_ID" -Method Delete -Headers $headers
        Write-Host "[OK] Message $MSG2_ID supprime" -ForegroundColor Green
    } catch {
        Show-Error $_
    }
} else {
    Write-Host "[IGNORE] Pas de message" -ForegroundColor Yellow
}

# =============================================
# TEST 12 : Verification finale
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TEST 12 : Verification finale (historique groupe)" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan

try {
    $final = Invoke-RestMethod -Uri "$BASE/member/chat/group/messages?page=0&size=10" -Headers $headers
    Write-Host "[OK] $($final.totalElements) messages restants" -ForegroundColor Green
    if ($final.content) {
        $final.content | ForEach-Object {
            $extra = ""
            if ($_.edited) { $extra += " [MODIFIE]" }
            if ($_.attachmentUrl) { $extra += " [FICHIER: $($_.attachmentUrl)]" }
            Write-Host "  -> [$($_.id)] $($_.sender.firstName): $($_.message)$extra"
        }
    }
} catch {
    Show-Error $_
}

# Nettoyage
if (Test-Path $testFile) { Remove-Item $testFile -Force }

# =============================================
# RESUME
# =============================================
Write-Host "$sep" -ForegroundColor Cyan
Write-Host "  TOUS LES TESTS TERMINES" -ForegroundColor Cyan
Write-Host "$sep" -ForegroundColor Cyan
