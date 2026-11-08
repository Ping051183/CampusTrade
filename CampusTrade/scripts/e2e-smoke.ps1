# ===== CampusTrade E2E smoke test =====
# Prereq: backend running on http://127.0.0.1:8080, DB loaded from init.sql + seed.sql
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e-smoke.ps1
param(
    [string]$BaseUrl = "http://127.0.0.1:8080"
)

Add-Type -AssemblyName System.Net.Http
$ErrorActionPreference = "Stop"
$script:passed = 0
$script:failed = 0

function Assert-True {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")
    if ($Condition) {
        $script:passed++
        Write-Output ("  [PASS] " + $Name + " " + $Detail)
    } else {
        $script:failed++
        Write-Output ("  [FAIL] " + $Name + " " + $Detail)
    }
}

function Post-Json {
    param([string]$Path, $Body, [string]$Token, [hashtable]$Headers)
    $h = @{ "Content-Type" = "application/json" }
    if ($Token) { $h["Authorization"] = "Bearer $Token" }
    if ($Headers) { foreach ($k in $Headers.Keys) { $h[$k] = $Headers[$k] } }
    $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Compress }
    return Invoke-RestMethod -Uri ($BaseUrl + $Path) -Method Post -Headers $h -Body ([System.Text.Encoding]::UTF8.GetBytes($json))
}

function Put-Json {
    param([string]$Path, [string]$Token)
    $h = @{}
    if ($Token) { $h["Authorization"] = "Bearer $Token" }
    return Invoke-RestMethod -Uri ($BaseUrl + $Path) -Method Put -Headers $h
}

function Get-Api {
    param([string]$Path, [string]$Token)
    $h = @{}
    if ($Token) { $h["Authorization"] = "Bearer $Token" }
    return Invoke-RestMethod -Uri ($BaseUrl + $Path) -Headers $h
}

function Login {
    param([string]$Username, [string]$Password)
    for ($i = 0; $i -lt 15; $i++) {
        try {
            $r = Post-Json "/api/auth/login" @{ username = $Username; password = $Password }
            if ($r.code -eq 200) { return $r.data }
        } catch {
            # 8004 rate-limited: wait for the fixed window to roll over
        }
        Start-Sleep -Seconds 7
    }
    throw "login failed for $Username"
}

Write-Output "== 1. login =="
$alice = Login "alice" "123456"
$bob = Login "bob" "123456"
$admin = Login "admin" "admin123"
$carol = Login "carol" "123456"
Assert-True "alice login token" ($null -ne $alice.token)
Assert-True "bob login token" ($null -ne $bob.token)
Assert-True "admin role is 0" ($admin.role -eq 0)
Assert-True "carol login token" ($null -ne $carol.token)

$sellerTokens = @{}
$sellerTokens[[int]$bob.userId] = $bob.token
$sellerTokens[[int]$carol.userId] = $carol.token

Write-Output "== 2. goods list =="
$list = (Get-Api "/api/goods/list?page=1&pageSize=20" $alice.token).data
Assert-True "goods list not empty" ($list.records.Count -gt 0) ("count=" + $list.records.Count)
$target = $list.records | Where-Object { $sellerTokens.ContainsKey([int]$_.sellerId) } | Select-Object -First 1
Assert-True "found goods sold by bob or carol" ($null -ne $target) ("goodsId=" + $target.id + " price=" + $target.price + " seller=" + $target.sellerId)
$sellerToken = $sellerTokens[[int]$target.sellerId]

Write-Output "== 3. create order + idempotency =="
$idemKey = "e2e-" + [guid]::NewGuid().ToString("N")
$order1 = (Post-Json ("/api/order/create/" + $target.id) @{} $alice.token @{ "Idempotency-Key" = $idemKey })
Assert-True "order created" ($order1.code -eq 200) ("orderId=" + $order1.data.id)
$orderId = $order1.data.id
$order2 = (Post-Json ("/api/order/create/" + $target.id) @{} $alice.token @{ "Idempotency-Key" = $idemKey })
Assert-True "idempotent replay returns same order" ($order2.data.id -eq $orderId) ("replay=" + $order2.data.id)

$listAfter = (Get-Api "/api/goods/list?page=1&pageSize=20" $alice.token).data
$stillOnSale = $listAfter.records | Where-Object { $_.id -eq $target.id }
Assert-True "goods taken off shelf after order" ($null -eq $stillOnSale)

Write-Output "== 4. pay freezes balance =="
$walletBefore = (Get-Api "/api/wallet" $alice.token).data
$pay = (Put-Json ("/api/order/pay/" + $orderId) $alice.token)
Assert-True "pay ok" ($pay.code -eq 200) ("msg=" + $pay.message)
$walletAfter = (Get-Api "/api/wallet" $alice.token).data
$frozenDelta = [double]$walletAfter.frozenAmount - [double]$walletBefore.frozenAmount
Assert-True "frozen delta equals price" ([math]::Abs($frozenDelta - [double]$target.price) -lt 0.01) ("frozenDelta=" + $frozenDelta)
$repeatPay = (Put-Json ("/api/order/pay/" + $orderId) $alice.token)
Assert-True "duplicate pay rejected" ($repeatPay.code -ne 200) ("code=" + $repeatPay.code)

Write-Output "== 5. ship + confirm settles to seller =="
$buyerShip = (Put-Json ("/api/order/ship/" + $orderId) $alice.token)
Assert-True "buyer cannot ship (403)" ($buyerShip.code -ne 200) ("code=" + $buyerShip.code)
$ship = (Put-Json ("/api/order/ship/" + $orderId) $sellerToken)
Assert-True "seller ship ok" ($ship.code -eq 200) ("msg=" + $ship.message)
$sellerBefore = (Get-Api "/api/wallet" $sellerToken).data
$confirm = (Put-Json ("/api/order/confirm/" + $orderId) $alice.token)
Assert-True "buyer confirm ok" ($confirm.code -eq 200) ("msg=" + $confirm.message)
$sellerAfter = (Get-Api "/api/wallet" $sellerToken).data
$creditDelta = [double]$sellerAfter.availableAmount - [double]$sellerBefore.availableAmount
Assert-True "seller credited with price" ([math]::Abs($creditDelta - [double]$target.price) -lt 0.01) ("creditDelta=" + $creditDelta)

Write-Output "== 6. reconcile =="
$reconcile = (Get-Api "/api/admin/wallet/reconcile" $admin.token)
$discrepancies = @($reconcile.data).Count
Assert-True "reconcile has no discrepancy" ($reconcile.code -eq 200 -and $discrepancies -eq 0) ("discrepancies=" + $discrepancies)

Write-Output "== 7. upload magic-byte check =="
$fakePath = Join-Path $env:TEMP "fake-image.jpg"
Set-Content -Path $fakePath -Value "this is not an image" -Encoding ASCII
$bytes = [System.IO.File]::ReadAllBytes($fakePath)
$boundary = [System.Guid]::NewGuid().ToString()
$client = New-Object System.Net.Http.HttpClient
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $alice.token)
$content = New-Object System.Net.Http.MultipartFormDataContent($boundary)
$fileContent = [System.Net.Http.ByteArrayContent]::new($bytes, 0, $bytes.Length)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/jpeg")
$content.Add($fileContent, "file", "fake.jpg")
$uploadResp = $client.PostAsync(($BaseUrl + "/api/file/upload"), $content).Result
$uploadBody = $uploadResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
Assert-True "fake jpg rejected by magic check" ($uploadBody.code -ne 200) ("code=" + $uploadBody.code + " msg=" + $uploadBody.message)

Write-Output "== 8. login rate limit =="
$limited = $false
for ($i = 1; $i -le 12; $i++) {
    try {
        $r = Post-Json "/api/auth/login" @{ username = "alice"; password = "wrong-password" }
        if ($r.code -eq 8004) { $limited = $true; Write-Output ("  request #" + $i + " rate limited"); break }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $body = $reader.ReadToEnd() | ConvertFrom-Json
            if ($body.code -eq 8004) { $limited = $true; Write-Output ("  request #" + $i + " rate limited"); break }
        }
    }
}
Assert-True "repeated wrong login hits rate limit 8004" $limited

Write-Output ""
Write-Output ("==== RESULT: PASS=" + $script:passed + " FAIL=" + $script:failed + " ====")
if ($script:failed -gt 0) { exit 1 }