# ===== CampusTrade MQ timeout-cancel check =====
# Prereq: backend started with campustrade.order.timeout-minutes=1 and scan-interval-ms=600000
# so only the RabbitMQ delay message can cancel the order (the @Scheduled fallback is 10 min away).
param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$WaitSeconds = 95
)

Add-Type -AssemblyName System.Net.Http
$ErrorActionPreference = "Stop"

function Login {
    param([string]$Username, [string]$Password)
    for ($i = 0; $i -lt 15; $i++) {
        try {
            $r = Invoke-RestMethod -Uri ($BaseUrl + "/api/auth/login") -Method Post -ContentType "application/json" `
                -Body ([System.Text.Encoding]::UTF8.GetBytes('{"username":"' + $Username + '","password":"' + $Password + '"}'))
            if ($r.code -eq 200) { return $r.data }
        } catch {
            $body = $null
            if ($_.Exception.Response) {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $body = $reader.ReadToEnd()
            }
            Write-Output ("  login retry #" + ($i + 1) + " " + $body)
        }
        Start-Sleep -Seconds 7
    }
    throw "login failed for $Username"
}

$me = Login "alice" "123456"
$token = $me.token
$h = @{ Authorization = "Bearer $token" }

$list = Invoke-RestMethod -Uri ($BaseUrl + "/api/goods/list?page=1&pageSize=20") -Headers $h
$target = $list.data.records | Select-Object -First 1
if (-not $target) { Write-Output "NO_ON_SALE_GOODS"; exit 1 }

$idem = "timeout-" + [guid]::NewGuid().ToString("N")
$order = Invoke-RestMethod -Uri ($BaseUrl + "/api/order/create/" + $target.id) -Method Post -Headers (@{
    Authorization = "Bearer $token"
    "Idempotency-Key" = $idem
}) -Body ([System.Text.Encoding]::UTF8.GetBytes("{}"))
$orderId = $order.data.id
Write-Output ("created order=" + $orderId + " no=" + $order.data.orderNo + " goods=" + $target.id + " status=" + $order.data.status)

Write-Output ("waiting " + $WaitSeconds + "s for the delay message to expire ...")
Start-Sleep -Seconds $WaitSeconds

$detail = Invoke-RestMethod -Uri ($BaseUrl + "/api/order/detail/" + $orderId) -Headers $h
Write-Output ("after wait: order status=" + $detail.data.status + " (4=cancelled)")
$goodsDetail = Invoke-RestMethod -Uri ($BaseUrl + "/api/goods/detail/" + $target.id) -Headers $h
Write-Output ("after wait: goods status=" + $goodsDetail.data.status + " (1=on sale)")

$ok = ($detail.data.status -eq 4) -and ($goodsDetail.data.status -eq 1)
Write-Output ("RESULT=" + $(if ($ok) { "PASS" } else { "FAIL" }))
if (-not $ok) { exit 1 }