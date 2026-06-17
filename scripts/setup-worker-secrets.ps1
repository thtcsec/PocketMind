# Usage: .\scripts\setup-worker-secrets.ps1 -ServiceAccountPath "C:\path\to\firebase-adminsdk.json"
param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceAccountPath
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$workerDir = Join-Path $root "pocketmind-admin-worker"
$gsPath = Join-Path $root "app\google-services.json"

if (-not (Test-Path $ServiceAccountPath)) {
    Write-Error "Service account file not found: $ServiceAccountPath"
}
if (-not (Test-Path $gsPath)) {
    Write-Error "google-services.json not found. Run: firebase apps:sdkconfig ANDROID <APP_ID> --out app/google-services.json"
}

$sa = Get-Content $ServiceAccountPath -Raw | ConvertFrom-Json
if (-not $sa.project_id -or -not $sa.client_email -or -not $sa.private_key) {
    Write-Error @"
Wrong file type. This is NOT a Firebase service account key.
Expected JSON with: project_id, client_email, private_key
You may have copied google-services.json by mistake.

Get the correct file:
  Firebase Console → Project settings → Service accounts → Generate new private key
  Save as: secrets/pocketmind-tuhoang-adminsdk.json
"@
}
$gs = Get-Content $gsPath -Raw | ConvertFrom-Json
$apiKey = $gs.client[0].api_key[0].current_key
$projectId = $sa.project_id
$clientEmail = $sa.client_email
$privateKey = $sa.private_key

Push-Location $workerDir
try {
    Write-Host "Setting Worker secrets for project $projectId ..."
    $projectId | npx wrangler secret put FIREBASE_PROJECT_ID
    $apiKey | npx wrangler secret put FIREBASE_API_KEY
    $clientEmail | npx wrangler secret put FIREBASE_CLIENT_EMAIL
    $privateKey | npx wrangler secret put FIREBASE_PRIVATE_KEY
    Write-Host "Done. Optional: npx wrangler secret put OPENAI_API_KEY"
    npx wrangler deploy
} finally {
    Pop-Location
}
