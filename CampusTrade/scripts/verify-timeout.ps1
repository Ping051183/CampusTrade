# Verify order timeout auto-cancel end to end (backend started with short timeout params).
# Usage: powershell -ExecutionPolicy Bypass -File scripts/verify-timeout.ps1
$ErrorActionPreference = 'Stop'
$mysql = 'C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe'
$env:MYSQL_PWD = '123456'
$base = 'http://localhost:8080/api'
$pass = 0; $fail = 0
function Check($name, $cond, $detail) {
    if ($cond) { $script:pass++; Write-Host ("PASS  {0}  {1}" -f $name, $detail) }
    else { $script:fail++; Write-Host ("FAIL  {0}  {1}" -f $name, $detail) }
}
function Sql($q) { (($( & $mysql -uroot campus_trade -N -B -e $q)) | Out-String).Trim() }
function Post($url, $body, $token) {
    $h = @{}
    if ($token) { $h['Authorization'] = "Bearer $token" }
    return Invoke-RestMethod -Uri ($base + $url) -Method Post -Headers $h -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
}

# 1. cleanup leftovers from previous runs
Sql "DELETE FROM notification WHERE related_id IN (SELECT id FROM orders WHERE remark LIKE 'timeout-probe%')"
Sql "DELETE FROM orders WHERE remark LIKE 'timeout-probe%'"
Sql "DELETE FROM goods WHERE title = 'timeout-probe'"

# 2. login buyer (lwx id=3), seller is user 4
$login = Post '/auth/login' '{"username":"lwx","password":"123456"}' $null
Check 'login' ($login.code -eq 200 -and $login.data.token) "code=$($login.code)"
$token = $login.data.token

# 3. publish probe goods owned by user 4, then place order
$gid = Sql "INSERT INTO goods (title, description, price, images, category_id, seller_id, status, create_time, update_time) VALUES ('timeout-probe','probe',10.00,'[]',1,4,1,NOW(),NOW()); SELECT LAST_INSERT_ID();"
$o = Post "/order/create/$gid" '{"remark":"timeout-probe"}' $token
Check 'create order' ($o.code -eq 200 -and $o.data.id) "orderId=$($o.data.id)"
$oid = $o.data.id

# 4. backdate create_time so the order is older than the 1-minute timeout
Sql "UPDATE orders SET create_time = NOW() - INTERVAL 5 MINUTE WHERE id = $oid"

# 5. wait for one scan cycle
Start-Sleep -Seconds 30

$status = Sql "SELECT status FROM orders WHERE id = $oid"
$goodsStatus = Sql "SELECT status FROM goods WHERE id = $gid"
$notifCount = Sql "SELECT COUNT(*) FROM notification WHERE related_id = $oid AND type = 'ORDER_TIMEOUT_CANCELLED'"
$notifUsers = Sql "SELECT GROUP_CONCAT(DISTINCT user_id ORDER BY user_id) FROM notification WHERE related_id = $oid AND type = 'ORDER_TIMEOUT_CANCELLED'"

Check 'order cancelled' ($status -eq '4') "status=$status (expect 4)"
Check 'goods restored' ($goodsStatus -eq '1') "status=$goodsStatus (expect 1)"
Check 'both notified' ($notifCount -eq '2') "count=$notifCount users=$notifUsers (expect 3,4)"

# 6. cleanup probe data
Sql "DELETE FROM notification WHERE related_id = $oid"
Sql "DELETE FROM orders WHERE id = $oid"
Sql "DELETE FROM goods WHERE id = $gid"

Write-Host ""
Write-Host ("RESULT: {0} passed, {1} failed" -f $pass, $fail)
if ($fail -gt 0) { exit 1 } else { exit 0 }