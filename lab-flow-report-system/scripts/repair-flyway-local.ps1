param(
    [string]$DbUrl = $env:DB_URL,
    [string]$DbUser = $env:DB_USER,
    [string]$DbPass = $env:DB_PASS,
    [switch]$StartApp
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($DbUrl)) {
    $DbUrl = 'jdbc:mysql://127.0.0.1:3306/lab_flow_report?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8&createDatabaseIfNotExist=true'
}

if ([string]::IsNullOrWhiteSpace($DbUser)) {
    $DbUser = 'root'
}

if ($null -eq $DbPass) {
    $DbPass = ''
}

$flywayUrl = $DbUrl
if ($flywayUrl.Contains('?')) {
    $flywayUrl = $flywayUrl.Substring(0, $flywayUrl.IndexOf('?'))
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$mvnw = Join-Path $repoRoot 'mvnw.cmd'

$repairArgs = @(
    'org.flywaydb:flyway-maven-plugin:11.7.2:repair'
    "-Dflyway.url=$flywayUrl"
    "-Dflyway.user=$DbUser"
    "-Dflyway.password=$DbPass"
    '-Dflyway.locations=filesystem:src/main/resources/db/migration'
)

Write-Host "Running Flyway repair against $flywayUrl with user '$DbUser'..."
& $mvnw @repairArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($StartApp) {
    Write-Host 'Flyway repair finished. Starting Spring Boot...'
    & $mvnw 'spring-boot:run'
    exit $LASTEXITCODE
}

Write-Host 'Flyway repair finished.'
