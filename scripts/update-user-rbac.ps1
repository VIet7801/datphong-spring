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
. (Join-Path $PSScriptRoot 'local-environment.ps1')
Push-Location $projectDirectory
try {
    [Console]::OutputEncoding = New-Object Text.UTF8Encoding($false)
    $JavaHome = Resolve-LocalJavaHome $JavaHome
    $env:PATH = "$JavaHome\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$originalPath"
    if (-not $JdbcJar) {
        $JdbcJar = Find-LocalJar @((Join-Path $projectDirectory 'target/schema-lib'), (Join-Path $env:USERPROFILE '.m2/repository/org/postgresql/postgresql')) 'postgresql-*.jar'
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
