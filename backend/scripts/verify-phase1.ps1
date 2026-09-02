# Phase 1 verification: start backend (java -jar), poll health, capture evidence,
# then stop the backend. Designed to run detached and write results to a file.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $root

# Load backend/.env (gitignored) into the process environment
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*\S)\s*$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }
}

$jar = Join-Path $root 'target\lokmit-foundation-backend-0.1.0-SNAPSHOT.jar'
$outLog = Join-Path $root 'verify-app.log'
$errLog = Join-Path $root 'verify-app.err.log'
$resultFile = Join-Path $root 'verify-result.json'
Remove-Item $outLog, $errLog, $resultFile -Force -ErrorAction SilentlyContinue

$java = (Get-Command java).Source
# Quote the jar path - the project path contains spaces.
$jarArg = '"' + $jar + '"'
$javaProc = Start-Process -FilePath $java -ArgumentList @('-jar', $jarArg) -WorkingDirectory $root -RedirectStandardOutput $outLog -RedirectStandardError $errLog -NoNewWindow -PassThru

$healthBody = $null
$httpStatus = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 4
    if ($javaProc.HasExited) { break }
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/health' -UseBasicParsing -TimeoutSec 4
        if ($r.StatusCode -eq 200) {
            $httpStatus = $r.StatusCode
            $healthBody = ($r.Content | ConvertFrom-Json) | ConvertTo-Json -Compress
            break
        }
    } catch { }
}

$flywayEvidence = ''
if (Test-Path $outLog) {
    $flywayEvidence = (@(Get-Content $outLog | Where-Object { $_ -match 'Flyway|Migrat|Successfully applied|Schema' }) -join "`n")
}

$result = [ordered]@{
    healthReached = ($null -ne $healthBody)
    httpStatus = $httpStatus
    healthBody = $healthBody
    appStarted = (Test-Path $outLog) -and ((Get-Content $outLog -Raw) -match 'Started LokmitFoundationApplication')
    flywayEvidence = $flywayEvidence
    javaExited = $javaProc.HasExited
}
$result | ConvertTo-Json -Depth 4 | Set-Content -Path $resultFile -Encoding UTF8

# Stop the backend
Stop-Process -Id $javaProc.Id -Force -ErrorAction SilentlyContinue
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}
Write-Output 'VERIFICATION_SCRIPT_COMPLETE'