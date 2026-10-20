param(
    [switch]$VerifyOnly,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JdbcJar,
    [string]$BCryptJar
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$originalPath = $env:PATH
$originalJavaHome = $env:JAVA_HOME
$originalOutputEncoding = $OutputEncoding
$originalConsoleEncoding = [Console]::OutputEncoding

function Find-DemoJavaHome {
    $candidates = @()
    if ($JavaHome) { $candidates += $JavaHome }
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
            $version = [IO.File]::ReadAllText($release)
            if ($version -match 'JAVA_VERSION="(\d+)' -and [int]$Matches[1] -ge 17) { return $candidate }
        }
    }
    throw 'Cần JDK 17+. Đặt JAVA_HOME hoặc truyền -JavaHome.'
}

function Find-DemoJar([string]$root, [string]$filter) {
    if (Test-Path -LiteralPath $root) {
        $jar = Get-ChildItem -LiteralPath $root -Recurse -Filter $filter |
            Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($jar) { return $jar.FullName }
    }
    return $null
}

Push-Location $projectDirectory
try {
    $utf8 = New-Object Text.UTF8Encoding($false)
    $OutputEncoding = $utf8
    [Console]::OutputEncoding = $utf8
    $env:JAVA_HOME = Find-DemoJavaHome
    $env:PATH = "$env:JAVA_HOME\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$originalPath"
    $auditDirectory = Join-Path $projectDirectory 'target/seed-audit'
    $libraryDirectory = Join-Path $projectDirectory 'target/seed-lib'
    New-Item -ItemType Directory -Force -Path $auditDirectory, $libraryDirectory | Out-Null
    if (-not $JdbcJar) { $JdbcJar = Find-DemoJar (Join-Path $projectDirectory 'target/schema-lib') 'postgresql-*.jar' }
    if (-not $JdbcJar) { $JdbcJar = Find-DemoJar (Join-Path $env:USERPROFILE '.m2/repository/org/postgresql/postgresql') 'postgresql-*.jar' }
    if (-not $BCryptJar) { $BCryptJar = Find-DemoJar (Join-Path $env:USERPROFILE '.m2/repository/org/mindrot/jbcrypt') 'jbcrypt-*.jar' }
    if (-not $JdbcJar) { $JdbcJar = Find-DemoJar $libraryDirectory 'postgresql-*.jar' }
    if (-not $BCryptJar) { $BCryptJar = Find-DemoJar $libraryDirectory 'jbcrypt-*.jar' }
    if (-not $JdbcJar -or -not $BCryptJar) {
        Write-Host 'Đang chuẩn bị JDBC/BCrypt bằng Maven Wrapper...'
        & (Join-Path $projectDirectory 'mvnw.cmd') '-B' '-ntp' 'dependency:copy-dependencies' '-DincludeGroupIds=org.postgresql,org.mindrot' "-DoutputDirectory=$libraryDirectory" *> (Join-Path $auditDirectory 'maven-seed-libs.log')
        if ($LASTEXITCODE -ne 0) { throw 'Không tải được thư viện. Xem target/seed-audit/maven-seed-libs.log.' }
        if (-not $JdbcJar) { $JdbcJar = Find-DemoJar $libraryDirectory 'postgresql-*.jar' }
        if (-not $BCryptJar) { $BCryptJar = Find-DemoJar $libraryDirectory 'jbcrypt-*.jar' }
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
