<#
.SYNOPSIS
  Minimal HTTP load generator: concurrent requests, QPS and P50/P95/P99 latency.

.DESCRIPTION
  No JMeter / wrk required; runs on Windows PowerShell 5.1 and PowerShell 7.
  Uses one shared HttpClient and keeps requests in flight with Task.WhenAny,
  so connection reuse is real and the closed-loop concurrency equals -Concurrency.

.PARAMETER Url
  Target endpoint, e.g. http://localhost:8080/api/goods?page=1

.PARAMETER Concurrency
  Number of requests kept in flight.

.PARAMETER DurationSeconds
  How long to keep sending.

.PARAMETER Token
  Optional JWT; sent as "Authorization: Bearer <token>".

.PARAMETER OutFile
  Optional path to write the JSON result.

.EXAMPLE
  ./scripts/bench.ps1 -Url http://localhost:8080/api/goods?page=1 -Concurrency 50 -DurationSeconds 20

.EXAMPLE
  ./scripts/bench.ps1 -Url http://localhost:8080/api/orders -Token $jwt -Concurrency 100 -DurationSeconds 30 -OutFile bench-orders.json
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Url,

    [int]$Concurrency = 50,

    [int]$DurationSeconds = 20,

    [string]$Method = 'GET',

    [string]$Token = '',

    [string]$OutFile = ''
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 does not load System.Net.Http implicitly
try { Add-Type -AssemblyName System.Net.Http -ErrorAction Stop } catch { }

$handler = $null
try {
    # .NET Core / PowerShell 7
    $handler = New-Object System.Net.Http.SocketsHttpHandler
    $handler.MaxConnectionsPerServer = $Concurrency * 2
} catch {
    # Windows PowerShell 5.1 (.NET Framework): connection pool is tuned globally
    $handler = New-Object System.Net.Http.HttpClientHandler
    [System.Net.ServicePointManager]::DefaultConnectionLimit = $Concurrency * 2
    [System.Net.ServicePointManager]::Expect100Continue = $false
}

$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(15)

if ($Token) {
    $client.DefaultRequestHeaders.Authorization =
        New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
}

$latencies = New-Object System.Collections.Generic.List[double]
$statusCounts = @{}
$errorMessages = New-Object System.Collections.Generic.List[string]

function New-RequestTask {
    param($client, $method, $url)
    $request = New-Object System.Net.Http.HttpRequestMessage((New-Object System.Net.Http.HttpMethod($method)), $url)
    return @{
        Task    = $client.SendAsync($request)
        Timer   = [System.Diagnostics.Stopwatch]::StartNew()
        Request = $request
    }
}

Write-Host ("Load test: {0} {1}  concurrency={2}  duration={3}s" -f $Method, $Url, $Concurrency, $DurationSeconds) -ForegroundColor Cyan

$inFlight = New-Object System.Collections.Generic.List[object]
$deadline = [DateTime]::UtcNow.AddSeconds($DurationSeconds)
$totalTimer = [System.Diagnostics.Stopwatch]::StartNew()

while ([DateTime]::UtcNow -lt $deadline -or $inFlight.Count -gt 0) {
    while ($inFlight.Count -lt $Concurrency -and [DateTime]::UtcNow -lt $deadline) {
        $inFlight.Add((New-RequestTask -client $client -method $Method -url $Url))
    }
    if ($inFlight.Count -eq 0) { break }

    $tasks = @($inFlight | ForEach-Object { $_.Task })
    $doneIndex = [System.Threading.Tasks.Task]::WaitAny($tasks, 2000)
    if ($doneIndex -lt 0) { continue }

    $entry = $inFlight[$doneIndex]
    $inFlight.RemoveAt($doneIndex)
    $entry.Timer.Stop()
    $latencies.Add($entry.Timer.Elapsed.TotalMilliseconds)

    try {
        $response = $entry.Task.GetAwaiter().GetResult()
        $null = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $status = [int]$response.StatusCode
        if ($statusCounts.ContainsKey($status)) { $statusCounts[$status]++ } else { $statusCounts[$status] = 1 }
        $response.Dispose()
    } catch {
        $errorMessages.Add($_.Exception.GetBaseException().Message)
    } finally {
        $entry.Request.Dispose()
    }
}

$totalTimer.Stop()
$client.Dispose()

$sorted = $latencies.ToArray() | Sort-Object
$total = $sorted.Count
if ($total -eq 0) {
    Write-Host 'No request completed; check that the service is running.' -ForegroundColor Red
    exit 1
}

function Get-Percentile {
    param([double[]]$Data, [double]$Percentile)
    $index = [Math]::Ceiling($Percentile / 100.0 * $Data.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $Data.Count) { $index = $Data.Count - 1 }
    return $Data[$index]
}

$elapsed = $totalTimer.Elapsed.TotalSeconds

# JSON needs string keys; integer status codes would break ConvertTo-Json
$statusCodes = @{}
foreach ($code in $statusCounts.Keys) { $statusCodes["$code"] = $statusCounts[$code] }

$result = [ordered]@{
    url             = $Url
    method          = $Method
    concurrency     = $Concurrency
    durationSeconds = [Math]::Round($elapsed, 2)
    requests        = $total
    qps             = [Math]::Round($total / $elapsed, 2)
    errorCount      = $errorMessages.Count
    errorRatePct    = [Math]::Round($errorMessages.Count / $total * 100, 2)
    latencyMs       = [ordered]@{
        min = [Math]::Round($sorted[0], 2)
        p50 = [Math]::Round((Get-Percentile $sorted 50), 2)
        p95 = [Math]::Round((Get-Percentile $sorted 95), 2)
        p99 = [Math]::Round((Get-Percentile $sorted 99), 2)
        max = [Math]::Round($sorted[$total - 1], 2)
    }
    statusCodes     = $statusCodes
}

Write-Host ''
Write-Host '========== Result ==========' -ForegroundColor Green
$result | ConvertTo-Json -Depth 5
Write-Host '============================' -ForegroundColor Green

if ($errorMessages.Count -gt 0) {
    Write-Host 'First errors:' -ForegroundColor Yellow
    $errorMessages | Select-Object -First 3 | ForEach-Object { Write-Host ("  " + $_) }
}

if ($OutFile) {
    $result | ConvertTo-Json -Depth 5 | Set-Content -Path $OutFile -Encoding UTF8
    Write-Host ("Result written to " + $OutFile) -ForegroundColor Cyan
}