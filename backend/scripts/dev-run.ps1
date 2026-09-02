# LOKMIT FOUNDATION backend - local dev helper.
# Loads backend/.env (gitignored) into the process environment and runs the
# Spring Boot application with Maven. Requires a local JDK 21 and Maven 3.9+.
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$envFile = Join-Path $root '.env'

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*\S)\s*$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
        }
    }
    Write-Host 'Loaded environment from backend/.env'
} else {
    Write-Warning 'backend/.env not found - DB_USERNAME/DB_PASSWORD must be exported manually.'
}

Push-Location $root
try {
    & mvn.cmd spring-boot:run
} finally {
    Pop-Location
}