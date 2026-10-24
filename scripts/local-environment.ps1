# Shared by the Windows local entry points. Never print the resolved Compose config.
function Resolve-LocalJavaHome([string]$preferred) {
    $candidates = @()
    if ($preferred) { $candidates += $preferred }
    else {
        $command = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $candidates += Split-Path (Split-Path $command.Source -Parent) -Parent }
        foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.jdks")) {
            if (Test-Path -LiteralPath $root) {
                $candidates += Get-ChildItem -LiteralPath $root -Directory | Sort-Object Name | Select-Object -ExpandProperty FullName
            }
        }
    }
    foreach ($candidate in $candidates) {
        $release = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -and (Test-Path -LiteralPath $release)) {
            if ([IO.File]::ReadAllText($release) -match 'JAVA_VERSION="(\d+)' -and [int]$Matches[1] -ge 17) { return $candidate }
        }
    }
    throw 'JDK 17+ is required. Set JAVA_HOME or pass -JavaHome.'
}

function Resolve-LocalDocker {
    $command = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    $default = "$env:ProgramFiles\Docker\Docker\resources\bin\docker.exe"
    if (Test-Path -LiteralPath $default) { return $default }
    throw 'Docker Desktop was not found. Install it and add docker.exe to PATH.'
}

function Find-LocalJar([string[]]$roots, [string]$filter) {
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $jar = Get-ChildItem -LiteralPath $root -Recurse -Filter $filter |
            Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($jar) { return $jar.FullName }
    }
    return $null
}

function Import-LocalComposeEnvironment([string]$docker) {
    $json = & $docker compose config --format json
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read Docker Compose configuration.' }
    $services = ($json -join "`n" | ConvertFrom-Json).services
    $settings = @{
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5435/' + [Uri]::EscapeDataString($services.postgres.environment.POSTGRES_DB)
        SPRING_DATASOURCE_USERNAME = $services.postgres.environment.POSTGRES_USER
        SPRING_DATASOURCE_PASSWORD = $services.postgres.environment.POSTGRES_PASSWORD
        SPRING_DATA_REDIS_HOST = 'localhost'
        SPRING_DATA_REDIS_PORT = '6379'
        SPRING_DATA_REDIS_PASSWORD = $services.redis.environment.REDIS_PASSWORD
        MINIO_ENDPOINT = 'http://localhost:9000'
        MINIO_ACCESS_KEY = $services.minio.environment.MINIO_ROOT_USER
        MINIO_SECRET_KEY = $services.minio.environment.MINIO_ROOT_PASSWORD
    }
    # These app-only settings are exposed by Compose's interpolation environment.
    $lines = & $docker compose config --environment
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read Compose environment.' }
    foreach ($line in $lines) {
        if ($line -match '^(JWT_SECRET|JWT_EXPIRATION_MS|MINIO_BUCKET)=(.*)$') {
            $settings[$Matches[1]] = $Matches[2]
        }
    }
    foreach ($name in $settings.Keys) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
        }
    }
}
