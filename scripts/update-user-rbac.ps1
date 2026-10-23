param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JdbcJar,
    [switch]$VerifyOnly,
    [switch]$SchemaOnly
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$originalPath = $env:PATH
$originalEncoding = [Console]::OutputEncoding
Push-Location $projectDirectory
try {
    [Console]::OutputEncoding = New-Object Text.UTF8Encoding($false)
    if (-not $JavaHome) {
        $candidates = @()
        foreach ($root in @("$env:ProgramFiles\Java", "$env:USERPROFILE\.jdks")) {
            if (Test-Path -LiteralPath $root) {
                $candidates += Get-ChildItem -LiteralPath $root -Directory | Select-Object -ExpandProperty FullName
            }
        }
        $JavaHome = $candidates | Where-Object {
            $releasePath = Join-Path $_ 'release'
            (Test-Path -LiteralPath (Join-Path $_ 'bin/javac.exe')) -and
            (Test-Path -LiteralPath $releasePath) -and
            ([IO.File]::ReadAllText($releasePath) -match 'JAVA_VERSION="(1[7-9]|[2-9][0-9])')
        } | Select-Object -First 1
    }
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/javac.exe'))) {
        throw 'JDK 17+ is required; pass -JavaHome or set JAVA_HOME.'
    }
    $env:PATH = "$JavaHome\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$originalPath"
    if (-not $JdbcJar) {
        $JdbcJar = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar') |
            Where-Object { $_.Name -notmatch '(sources|javadoc)' } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $JdbcJar -or -not (Test-Path -LiteralPath $JdbcJar)) {
        throw 'PostgreSQL JDBC is missing; run .\mvnw.cmd compile or pass -JdbcJar.'
    }
    $classes = Join-Path $projectDirectory 'target/rbac-tools'
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    & (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -d $classes 'scripts/java/SchemaTool.java' 'scripts/java/RbacCatalogTool.java'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot compile the RBAC tool.' }
    if ($SchemaOnly) {
        if ($VerifyOnly) { throw 'Use Maven EntitySchemaTest to validate the schema.' }
        & (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' -cp "$classes;$JdbcJar" SchemaTool apply 'scripts/sql/V006__runtime_schema_alignment.sql'
        if ($LASTEXITCODE -ne 0) { throw 'Runtime schema update failed.' }
        & (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' -cp "$classes;$JdbcJar" SchemaTool apply 'scripts/sql/V007__refund_request_idempotency.sql'
    } else {
        $mode = if ($VerifyOnly) { 'verify' } else { 'apply' }
        & (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' -cp "$classes;$JdbcJar" RbacCatalogTool $mode
    }
    if ($LASTEXITCODE -ne 0) { throw 'Database update/verification failed.' }
}
finally {
    Pop-Location
    $env:PATH = $originalPath
    [Console]::OutputEncoding = $originalEncoding
}
