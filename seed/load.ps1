# Load the synthetic demo dataset into an already migrated database.
# Usage: seed/load.ps1 [-Database recruiting]
param([string]$Database = 'recruiting')
# Windows PowerShell 5.1 turns docker's stderr progress lines into terminating errors under 'Stop',
# so native calls are checked through $LASTEXITCODE instead.
$ErrorActionPreference = 'Continue'
Set-Location (Join-Path $PSScriptRoot '..')
docker compose exec -T postgres rm -rf /tmp/seed
if ($LASTEXITCODE -ne 0) { throw 'docker compose exec failed' }
docker compose cp seed postgres:/tmp/seed
if ($LASTEXITCODE -ne 0) { throw 'docker compose cp failed' }
docker compose exec -T postgres sh -c "cd /tmp/seed && psql -U recruiting -d '$Database' -v ON_ERROR_STOP=1 -q -f run.sql"
if ($LASTEXITCODE -ne 0) { throw 'seed load failed' }
Write-Host "Demo data loaded into '$Database'. Log in as admin@demo.example / manager@demo.example / consultant@demo.example (Demo-Password-2026)."
