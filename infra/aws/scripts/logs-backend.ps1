param (
    [string]$StackName = "guantes-backend-stack",
    [string]$Region = "us-east-1",
    [int]$Lines = 100
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Consulta de Logs de Contenedor guantes-backend via SSM" -ForegroundColor Cyan
Write-Host " Stack:   $StackName" -ForegroundColor Cyan
Write-Host " Region:  $Region" -ForegroundColor Cyan
Write-Host " Lineas:  $Lines" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$instanceId = (aws cloudformation describe-stacks `
    --stack-name "$StackName" `
    --region "$Region" `
    --query "Stacks[0].Outputs[?OutputKey=='IdInstancia'].OutputValue" `
    --output text).Trim()

if ([string]::IsNullOrWhiteSpace($instanceId) -or $instanceId -eq "None") {
    Write-Error "Error: No se pudo obtener el IdInstancia del stack $StackName."
    exit 1
}

Write-Host "Consultando logs en instancia $instanceId..." -ForegroundColor Yellow

$commandId = (aws ssm send-command `
    --instance-ids "$instanceId" `
    --document-name "AWS-RunShellScript" `
    --parameters "commands=[`"docker logs --tail $Lines guantes-backend 2>&1 || journalctl -u guantes-backend -n $Lines --no-pager`"]" `
    --comment "Consulta de logs guantes-backend" `
    --region "$Region" `
    --query "Command.CommandId" `
    --output text).Trim()

aws ssm wait command-executed `
    --command-id "$commandId" `
    --instance-id "$instanceId" `
    --region "$Region"

$logContent = aws ssm get-command-invocation `
    --command-id "$commandId" `
    --instance-id "$instanceId" `
    --region "$Region" `
    --query "StandardOutputContent" `
    --output text

Write-Host ""
Write-Host "=== Salida de Logs ===" -ForegroundColor Green
Write-Output $logContent
