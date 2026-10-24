param(
    [ValidateSet('setup', 'run')][string]$Action = 'setup',
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Seed
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'local-environment.ps1')
$projectDirectory = Split-Path $PSScriptRoot -Parent
$savedEnvironment = @{}
Get-ChildItem Env: | ForEach-Object { $savedEnvironment[$_.Name] = $_.Value }
Push-Location $projectDirectory
try {
    $env:JAVA_HOME = Resolve-LocalJavaHome $JavaHome
    $env:PATH = "$env:JAVA_HOME\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$env:PATH"
    $docker = Resolve-LocalDocker
    if (-not (Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
    Import-LocalComposeEnvironment $docker
    if ($Action -eq 'run') {
        & .\mvnw.cmd 'spring-boot:run'
        if ($LASTEXITCODE -ne 0) { throw 'Application startup failed. Check the last Caused by in the log.' }
    } else {
        & $docker compose up -d --wait
        if ($LASTEXITCODE -ne 0) { throw 'Dependencies did not start. Check Docker Desktop and occupied ports.' }
        & .\mvnw.cmd '-B' '-ntp' 'package'
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed. Database initialization has not run.' }
        foreach ($script in @('khoi-tao-db.cmd', 'cap-nhat-schema.cmd', 'cap-nhat-rbac.cmd')) {
            & (Join-Path $projectDirectory $script) '-JavaHome' $env:JAVA_HOME
            if ($LASTEXITCODE -ne 0) { throw "$script failed. Stopping setup." }
        }
        & .\cap-nhat-rbac.cmd '-JavaHome' $env:JAVA_HOME '-VerifyOnly'
        if ($LASTEXITCODE -ne 0) { throw 'RBAC verification failed.' }
        if ($Seed) {
            & .\tao-du-lieu-test.cmd '-JavaHome' $env:JAVA_HOME
            if ($LASTEXITCODE -ne 0) { throw 'Demo data initialization failed.' }
        }
        Write-Host 'Local setup complete. Run .\chay-local.cmd to start the backend.'
    }
} catch {
    Write-Host ("ERROR: " + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    foreach ($item in @(Get-ChildItem Env:)) {
        if (-not $savedEnvironment.ContainsKey($item.Name)) { [Environment]::SetEnvironmentVariable($item.Name, $null, 'Process') }
    }
    foreach ($name in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
    Pop-Location
}
