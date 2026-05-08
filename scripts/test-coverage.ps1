$ErrorActionPreference = "Stop"

Write-Host "Running tests and generating JaCoCo coverage report..." -ForegroundColor Cyan

./gradlew clean test jacocoTestReport

$reportPath = Join-Path $PSScriptRoot "../build/reports/jacoco/test/html/index.html"
$resolvedReportPath = Resolve-Path $reportPath

Write-Host "Opening coverage report:" -ForegroundColor Green
Write-Host $resolvedReportPath

Start-Process $resolvedReportPath
