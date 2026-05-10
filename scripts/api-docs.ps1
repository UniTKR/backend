param(
    [switch]$NoOpen
)

$ErrorActionPreference = "Stop"

Write-Host "Generating Spring REST Docs HTML..." -ForegroundColor Cyan

./gradlew asciidoctor

$docsPath = Join-Path $PSScriptRoot "../build/docs/asciidoc/index.html"
$resolvedDocsPath = Resolve-Path $docsPath

Write-Host "API docs generated:" -ForegroundColor Green
Write-Host $resolvedDocsPath

if (-not $NoOpen) {
    Write-Host "Opening API docs..." -ForegroundColor Green
    Start-Process $resolvedDocsPath
}
