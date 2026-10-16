param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JdbcJar
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
$originalOutputEncoding = $OutputEncoding
$originalConsoleEncoding = [Console]::OutputEncoding
$datasourceNames = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')
$originalDatasource = @{}
foreach ($name in $datasourceNames) { $originalDatasource[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }

function Find-JavaHome {
    if ($JavaHome) { $candidates = @($JavaHome) }
    else {
        $candidates = @()
        $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($javaCommand) { $candidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent }
        foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.jdks")) {
            if (Test-Path -LiteralPath $root) {
                $candidates += Get-ChildItem -LiteralPath $root -Directory | Sort-Object Name | Select-Object -ExpandProperty FullName
            }
        }
    }
    foreach ($candidate in $candidates) {
        $releaseFile = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -and (Test-Path -LiteralPath $releaseFile)) {
            $release = [IO.File]::ReadAllText($releaseFile)
            if ($release -match 'JAVA_VERSION="(\d+)' -and [int]$Matches[1] -ge 17) { return $candidate }
        }
    }
    throw 'Không tìm thấy JDK 17 trở lên. Hãy cài JDK và đặt JAVA_HOME, hoặc truyền -JavaHome vào script.'
}

function Read-DatasourceSetting([string]$name) {
    $override = [Environment]::GetEnvironmentVariable($name, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($override)) { return $override }
    $match = [regex]::Match($script:configuration, ([regex]::Escape($name) + ':([^}]+)'))
    if (-not $match.Success) { throw "Thiếu cấu hình $name trong application.yaml." }
    return $match.Groups[1].Value
}

function Test-DatabasePort([Uri]$address) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $pending = $client.ConnectAsync($address.Host, $address.Port)
        return ($pending.Wait(2000) -and $client.Connected)
    } catch { return $false }
    finally { $client.Dispose() }
}

Push-Location $projectDirectory
try {
    $utf8 = New-Object Text.UTF8Encoding($false)
    [Console]::OutputEncoding = $utf8
    $OutputEncoding = $utf8
    $env:JAVA_HOME = Find-JavaHome
    $env:PATH = "$env:JAVA_HOME\bin;$env:SystemRoot\System32;$env:SystemRoot\System32\WindowsPowerShell\v1.0;$originalPath"
    $javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe'
    $script:configuration = [IO.File]::ReadAllText((Join-Path $projectDirectory 'src/main/resources/application.yaml'))
    $url = Read-DatasourceSetting 'SPRING_DATASOURCE_URL'
    if (-not $url.StartsWith('jdbc:postgresql://')) { throw 'Script yêu cầu URL dạng jdbc:postgresql://host:port/database.' }
    $address = [Uri]$url.Substring(5)
    $localComposeAddress = $address.Host -in @('localhost', '127.0.0.1', '[::1]') -and $address.Port -eq 5435
    $dockerCommand = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    $dockerExecutable = if ($dockerCommand) { $dockerCommand.Source } else { "$env:ProgramFiles\Docker\Docker\resources\bin\docker.exe" }
    $useComposeConfiguration = $false

    # Dùng PostgreSQL đang chạy; nếu cổng mặc định chưa mở thì khởi động dịch vụ trong Docker Compose.
    if (-not (Test-DatabasePort $address)) {
        if (-not $localComposeAddress) {
            throw 'Không kết nối được PostgreSQL theo SPRING_DATASOURCE_URL. Hãy khởi động đúng máy chủ/cổng đã cấu hình.'
        }
        if (-not (Test-Path -LiteralPath $dockerExecutable)) {
            throw 'PostgreSQL tại localhost:5435 chưa chạy. Hãy cài và mở Docker Desktop, hoặc khởi động PostgreSQL của bạn.'
        }
        Write-Host 'Đang khởi động PostgreSQL bằng Docker Compose...'
        & $dockerExecutable compose up -d --wait postgres
        if ($LASTEXITCODE -ne 0) { throw 'Không khởi động được PostgreSQL. Hãy kiểm tra Docker Desktop và cấu hình .env.' }
        $useComposeConfiguration = $true
    } elseif ($localComposeAddress -and (Test-Path -LiteralPath $dockerExecutable)) {
        # Khi container đã chạy từ trước, vẫn dùng đúng thông tin .env cho lần chạy lại.
        $previousErrorPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $containerId = & $dockerExecutable compose ps -q --status running postgres 2>$null
            $useComposeConfiguration = $LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace(($containerId -join ''))
        } finally { $ErrorActionPreference = $previousErrorPreference }
    }

    if ($useComposeConfiguration) {
        # Đọc cấu hình Compose trong bộ nhớ để dùng đúng thông tin .env, không in mật khẩu.
        $composeJson = & $dockerExecutable compose config --format json
        if ($LASTEXITCODE -ne 0) { throw 'Không đọc được cấu hình Docker Compose.' }
        $postgresEnvironment = ($composeJson -join "`n" | ConvertFrom-Json).services.postgres.environment
        $composeSettings = @{
            SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5435/' + [Uri]::EscapeDataString($postgresEnvironment.POSTGRES_DB)
            SPRING_DATASOURCE_USERNAME = $postgresEnvironment.POSTGRES_USER
            SPRING_DATASOURCE_PASSWORD = $postgresEnvironment.POSTGRES_PASSWORD
        }
        foreach ($name in $datasourceNames) {
            if ([string]::IsNullOrWhiteSpace($originalDatasource[$name])) {
                # Giữ URL người dùng đã chọn; Compose chỉ cung cấp URL mặc định khi chưa có biến môi trường.
                if ($name -eq 'SPRING_DATASOURCE_URL' -and $address.AbsolutePath -ne '/vivu') { continue }
                [Environment]::SetEnvironmentVariable($name, $composeSettings[$name], 'Process')
            }
        }
    }

    $auditDirectory = Join-Path $projectDirectory 'target/schema-audit'
    New-Item -ItemType Directory -Force -Path $auditDirectory | Out-Null
    if (-not $JdbcJar) {
        $driverDirectory = Join-Path $projectDirectory 'target/schema-lib'
        $driver = Get-ChildItem -LiteralPath $driverDirectory -Filter 'postgresql-*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $driver) {
            Write-Host 'Đang chuẩn bị JDBC bằng Maven Wrapper; lần đầu cần Internet...'
            $mavenArguments = @('-B', '-ntp', 'dependency:copy-dependencies', '-DincludeGroupIds=org.postgresql', '-DincludeArtifactIds=postgresql', '-DincludeScope=runtime', "-DoutputDirectory=$driverDirectory")
            & (Join-Path $projectDirectory 'mvnw.cmd') @mavenArguments *> (Join-Path $auditDirectory 'maven-driver.log')
            if ($LASTEXITCODE -ne 0) { throw 'Không tải được JDBC. Xem target/schema-audit/maven-driver.log.' }
            $driver = Get-ChildItem -LiteralPath $driverDirectory -Filter 'postgresql-*.jar' | Select-Object -First 1
        }
        if (-not $driver) { throw 'Maven chưa tạo được tệp JDBC PostgreSQL.' }
        $JdbcJar = $driver.FullName
    }
    if (-not (Test-Path -LiteralPath $JdbcJar -PathType Leaf)) { throw 'Không tìm thấy tệp JAR được truyền qua -JdbcJar.' }

    Write-Host 'Đang kiểm tra và khởi tạo cấu trúc DB...'
    & $javaExecutable '-Dfile.encoding=UTF-8' -cp $JdbcJar 'scripts/java/SchemaTool.java' initialize
    if ($LASTEXITCODE -ne 0) { throw 'Khởi tạo thất bại. Xem thông báo phía trên để xử lý nguyên nhân.' }
    Write-Host 'Hoàn tất. Có thể chạy ứng dụng với ddl-auto: validate.'
} catch {
    Write-Host ("Lỗi: " + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    foreach ($name in $datasourceNames) { [Environment]::SetEnvironmentVariable($name, $originalDatasource[$name], 'Process') }
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
    $OutputEncoding = $originalOutputEncoding
    [Console]::OutputEncoding = $originalConsoleEncoding
    Pop-Location
}
