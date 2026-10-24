param(
    [switch]$VerifyOnly,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JdbcJar,
    [string]$BCryptJar
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'local-environment.ps1')
$originalPath = $env:PATH
$originalJavaHome = $env:JAVA_HOME
$originalOutputEncoding = $OutputEncoding
$originalConsoleEncoding = [Console]::OutputEncoding

Push-Location $projectDirectory
try {
    $utf8 = New-Object Text.UTF8Encoding($false)
    $OutputEncoding = $utf8
    [Console]::OutputEncoding = $utf8
    $env:JAVA_HOME = Resolve-LocalJavaHome $JavaHome
    $env:PATH = "$env:JAVA_HOME\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$originalPath"
    $auditDirectory = Join-Path $projectDirectory 'target/seed-audit'
    $libraryDirectory = Join-Path $projectDirectory 'target/seed-lib'
    New-Item -ItemType Directory -Force -Path $auditDirectory, $libraryDirectory | Out-Null
    if (-not $JdbcJar) {
        $JdbcJar = Find-LocalJar @((Join-Path $projectDirectory 'target/schema-lib'), (Join-Path $env:USERPROFILE '.m2/repository/org/postgresql/postgresql'), $libraryDirectory) 'postgresql-*.jar'
    }
    if (-not $BCryptJar) {
        $BCryptJar = Find-LocalJar @((Join-Path $env:USERPROFILE '.m2/repository/org/mindrot/jbcrypt'), $libraryDirectory) 'jbcrypt-*.jar'
    }
    if (-not $JdbcJar -or -not $BCryptJar) {
        Write-Host 'Đang chuẩn bị JDBC/BCrypt bằng Maven Wrapper...'
        & (Join-Path $projectDirectory 'mvnw.cmd') '-B' '-ntp' 'dependency:copy-dependencies' '-DincludeGroupIds=org.postgresql,org.mindrot' "-DoutputDirectory=$libraryDirectory" *> (Join-Path $auditDirectory 'maven-seed-libs.log')
        if ($LASTEXITCODE -ne 0) { throw 'Không tải được thư viện. Xem target/seed-audit/maven-seed-libs.log.' }
        if (-not $JdbcJar) { $JdbcJar = Find-LocalJar $libraryDirectory 'postgresql-*.jar' }
        if (-not $BCryptJar) { $BCryptJar = Find-LocalJar $libraryDirectory 'jbcrypt-*.jar' }
    }
    foreach ($jar in @($JdbcJar, $BCryptJar)) {
        if (-not $jar -or -not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Thiếu JDBC hoặc BCrypt JAR.' }
    }
    $command = if ($VerifyOnly) { 'verify' } else { 'seed' }
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') '-Dfile.encoding=UTF-8' '-Djava.awt.headless=true' '-cp' "$JdbcJar;$BCryptJar" 'scripts/java/DemoDataTool.java' $command
    if ($LASTEXITCODE -ne 0) { throw 'Dữ liệu test chưa hoàn tất; xem lỗi phía trên.' }
} catch {
    Write-Host ("Lỗi: " + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    $env:PATH = $originalPath
    $env:JAVA_HOME = $originalJavaHome
    $OutputEncoding = $originalOutputEncoding
    [Console]::OutputEncoding = $originalConsoleEncoding
    Pop-Location
}
