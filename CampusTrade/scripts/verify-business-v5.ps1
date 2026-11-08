# 第五轮功能验收脚本：评价/信用、退款、学号校验、系统通知
# 前置：后端已在 http://localhost:8080 运行，MySQL campus_trade 可写
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080/api'
$mysql = 'C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe'
$results = New-Object System.Collections.Generic.List[string]
function Ok($name, $cond, $detail) {
  $tag = if ($cond) { 'PASS' } else { 'FAIL' }
  $results.Add("[$tag] $name : $detail")
}
function Login($u, $p) {
  $r = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' -Body (@{ username = $u; password = $p } | ConvertTo-Json)
  return $r.data.token
}
function Api($method, $path, $token, $body) {
  $h = @{}
  if ($token) { $h['Authorization'] = "Bearer $token" }
  $args = @{ Method = $method; Uri = "$base$path"; Headers = $h }
  if ($body) { $args['ContentType'] = 'application/json; charset=utf-8'; $args['Body'] = [System.Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 6)) }
  try { return Invoke-RestMethod @args } catch {
    $resp = $_.ErrorDetails.Message
    if ($resp) { return ($resp | ConvertFrom-Json) }
    return [pscustomobject]@{ code = 0; message = $_.Exception.Message; data = $null }
  }
}
function Sql($q) { (& $mysql --user=root --password=123456 --host=127.0.0.1 --database=campus_trade --execute=$q --silent 2>$null | Out-String).Trim() }

# ---------- 0. 修复历史残留 + 登录 ----------
Sql "UPDATE goods SET status=1 WHERE id IN (2,3); DELETE FROM orders WHERE id IN (9,10);"
$adminToken = Login 'admin' 'admin123'
$lwxToken = Login 'lwx' '123456'
Ok '登录 admin / lwx' ($adminToken -and $lwxToken) '两个账号均拿到 JWT'

# ---------- 1. 学号 + 学校校验 ----------
$suffix = Get-Random -Maximum 99999
$bad = Api Post '/auth/register' $null @{ username = "vbad$suffix"; password = '123456'; nickname = '测试'; studentNo = '12345'; school = '广州工商学院' }
Ok '非法学号被拒绝' ($bad.code -eq 1007 -or $bad.code -eq 400) "code=$($bad.code) msg=$($bad.message)"

$wrongSchool = Api Post '/auth/register' $null @{ username = "vschool$suffix"; password = '123456'; nickname = '测试'; studentNo = '202310610320'; school = '其他大学' }
Ok '非本校被拒绝' ($wrongSchool.code -eq 1008) "code=$($wrongSchool.code) msg=$($wrongSchool.message)"

$goodNo = '2023' + (Get-Random -Minimum 100000 -Maximum 999999) + '01'
$reg = Api Post '/auth/register' $null @{ username = "vok$suffix"; password = '123456'; nickname = '测试'; studentNo = $goodNo; school = '广州工商学院' }
Ok '合法学号注册成功' ($reg.code -eq 200) "code=$($reg.code) studentNo=$goodNo"
Sql "DELETE FROM user WHERE username IN ('vok$suffix','vbad$suffix','vschool$suffix');"

# ---------- 2. 完整交易 + 评价 + 通知（lwx 卖 -> admin 买）----------
Sql "INSERT INTO goods (title, description, category_id, price, seller_id, images, status) VALUES ('验收交易商品','脚本自动创建',1,12.50,3,'[]',1);"
$gA = Sql "SELECT id FROM goods WHERE title='验收交易商品' ORDER BY id DESC LIMIT 1;"
$create = Api Post "/order/create/$gA" $adminToken @{ remark = '验收脚本订单' }
$orderId = $create.data.id
Ok '下单' ($create.code -eq 200 -and $orderId) "code=$($create.code) orderId=$orderId"

$pay = Api Put "/order/pay/$orderId" $adminToken $null
Ok '模拟支付 0->1' ($pay.code -eq 200 -and (Sql "SELECT status FROM orders WHERE id=$orderId;") -eq '1') "code=$($pay.code)"

$payAgain = Api Put "/order/pay/$orderId" $adminToken $null
Ok '重复支付被拒绝' ($payAgain.code -eq 5005) "code=$($payAgain.code) msg=$($payAgain.message)"

$ship = Api Put "/order/ship/$orderId" $lwxToken $null
Ok '卖家发货 1->2' ($ship.code -eq 200 -and (Sql "SELECT status FROM orders WHERE id=$orderId;") -eq '2') "code=$($ship.code) msg=$($ship.message)"

$confirm = Api Put "/order/confirm/$orderId" $adminToken $null
Ok '买家确认收货 2->3' ($confirm.code -eq 200 -and (Sql "SELECT status FROM orders WHERE id=$orderId;") -eq '3') "code=$($confirm.code)"

$review = Api Post '/review' $adminToken @{ orderId = $orderId; score = 5; content = '验收脚本：交易顺利' }
Ok '买家评价卖家' ($review.code -eq 200) "code=$($review.code) msg=$($review.message)"

$dup = Api Post '/review' $adminToken @{ orderId = $orderId; score = 1; content = '重复评价' }
Ok '重复评价被拒绝' ($dup.code -eq 5009) "code=$($dup.code) msg=$($dup.message)"

$summary = Api Get '/review/summary/3' $adminToken $null
Ok '信用分聚合' ($summary.code -eq 200 -and $summary.data.count -ge 1) "avgScore=$($summary.data.avgScore) count=$($summary.data.count) goodRate=$($summary.data.goodRate)%"

$sellerRated = Api Post '/review' $lwxToken @{ orderId = $orderId; score = 5; content = '买家爽快' }
Ok '卖家评价买家' ($sellerRated.code -eq 200) "code=$($sellerRated.code)"

$notices = Api Get '/notification/list?page=1&pageSize=20' $lwxToken $null
$types = ($notices.data.records | ForEach-Object { $_.type }) -join ','
Ok '卖家收到系统通知' ($notices.code -eq 200 -and $notices.data.total -ge 3) "total=$($notices.data.total) types=$types"

$unread = Api Get '/notification/unread-count' $lwxToken $null
Ok '未读数接口' ($unread.code -eq 200) "unread=$($unread.data)"

# ---------- 3. 退款闭环 ----------
Sql "INSERT INTO goods (title, description, category_id, price, seller_id, images, status) VALUES ('退款测试商品','脚本自动创建',1,1.00,3,'[]',1);"
$gB = Sql "SELECT id FROM goods WHERE title='退款测试商品' ORDER BY id DESC LIMIT 1;"
$c2 = Api Post "/order/create/$gB" $adminToken @{ remark = '退款测试' }
$oid2 = $c2.data.id
$p2 = Api Put "/order/pay/$oid2" $adminToken $null
$refund = Api Post "/order/refund/$oid2`?reason=不想要了" $adminToken $null
Ok '买家申请退款' ($refund.code -eq 200) "code=$($refund.code) orderId=$oid2"
$blockShip = Api Put "/order/ship/$oid2" $lwxToken $null
Ok '退款中禁止发货' ($blockShip.code -eq 5008) "code=$($blockShip.code) msg=$($blockShip.message)"
$agree = Api Put "/order/refund/agree/$oid2" $lwxToken $null
Ok '卖家同意退款' ($agree.code -eq 200) "code=$($agree.code)"
$orderState = Sql "SELECT CONCAT(status,'/',refund_status) FROM orders WHERE id=$oid2;"
$goodsState = Sql "SELECT status FROM goods WHERE id=$gB;"
Ok '退款后订单取消且商品恢复在售' ($orderState -eq '4/2' -and $goodsState -eq '1') "order=$orderState goods.status=$goodsState"

# ---------- 4. 清理测试数据 ----------
Sql "DELETE FROM review WHERE order_id IN ($orderId,$oid2); DELETE FROM notification WHERE related_id IN ($orderId,$oid2); DELETE FROM orders WHERE id IN ($orderId,$oid2); DELETE FROM goods WHERE id IN ($gA,$gB);"

$results | ForEach-Object { Write-Host $_ }
$pass = ($results | Where-Object { $_ -like '`[PASS`]*' }).Count
Write-Host "`n== $pass / $($results.Count) PASS =="