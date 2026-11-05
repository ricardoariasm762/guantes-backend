param (
    [string]$StackName = "guantes-backend-stack",
    [string]$Region = "us-east-2"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $scriptDir))

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Publicacion de Imagen Backend a Amazon ECR y EC2" -ForegroundColor Cyan
Write-Host " Stack:  $StackName" -ForegroundColor Cyan
Write-Host " Region: $Region" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Obtener salidas de CloudFormation (UriECR, IdInstancia, DnsPublicoEC2)
Write-Host "Consultando datos de despliegue desde CloudFormation..." -ForegroundColor Yellow
$outputsRaw = aws cloudformation describe-stacks `
    --stack-name "$StackName" `
    --region "$Region" `
    --query "Stacks[0].Outputs" `
    --output json | ConvertFrom-Json

$ecrUri = ($outputsRaw | Where-Object { $_.OutputKey -eq "UriECR" }).OutputValue
$instanceId = ($outputsRaw | Where-Object { $_.OutputKey -eq "IdInstancia" }).OutputValue
$ec2Dns = ($outputsRaw | Where-Object { $_.OutputKey -eq "DnsPublicoEC2" }).OutputValue

if ([string]::IsNullOrWhiteSpace($ecrUri) -or [string]::IsNullOrWhiteSpace($instanceId)) {
    Write-Error "Error: No se pudieron obtener UriECR o IdInstancia del stack $StackName."
    exit 1
}

Write-Host "-> Repositorio ECR: $ecrUri" -ForegroundColor Green
Write-Host "-> Instancia EC2:   $instanceId" -ForegroundColor Green
Write-Host "-> DNS Publico EC2: $ec2Dns" -ForegroundColor Green

# 2. Obtener SHA corto del commit actual
$commitSha = (git -C "$backendDir" rev-parse --short HEAD).Trim()
Write-Host "-> Version Commit:  $commitSha" -ForegroundColor Green

# 3. Construir la imagen Docker para linux/amd64
Write-Host ""
Write-Host "Construyendo imagen Docker para linux/amd64..." -ForegroundColor Yellow
docker build `
    --platform linux/amd64 `
    -t "${ecrUri}:${commitSha}" `
    -t "${ecrUri}:latest" `
    "$backendDir"

if ($LASTEXITCODE -ne 0) {
    Write-Error "Error durante la construccion de la imagen Docker."
    exit 1
}

# 4. Iniciar sesion en Amazon ECR y publicar imagenes
Write-Host ""
Write-Host "Iniciando sesion en Amazon ECR..." -ForegroundColor Yellow
$loginPass = (aws ecr get-login-password --region "$Region").Trim()
$loginPass | docker login --username AWS --password-stdin "$ecrUri"

Write-Host "Publicando imagen ${ecrUri}:${commitSha}..." -ForegroundColor Yellow
docker push "${ecrUri}:${commitSha}"

Write-Host "Publicando etiqueta ${ecrUri}:latest..." -ForegroundColor Yellow
docker push "${ecrUri}:latest"

# 5. Reiniciar servicio en EC2 usando AWS SSM (sin SSH)
Write-Host ""
Write-Host "Reiniciando servicio guantes-backend en EC2 mediante AWS SSM..." -ForegroundColor Yellow
$commandId = (aws ssm send-command `
    --instance-ids "$instanceId" `
    --document-name "AWS-RunShellScript" `
    --parameters 'commands=["systemctl restart guantes-backend"]' `
    --comment "Reinicio por despliegue de imagen $commitSha" `
    --region "$Region" `
    --query "Command.CommandId" `
    --output text).Trim()

Write-Host "-> Comando SSM enviado: $commandId. Esperando ejecucion..." -ForegroundColor Yellow
aws ssm wait command-executed `
    --command-id "$commandId" `
    --instance-id "$instanceId" `
    --region "$Region"

Write-Host "-> Servicio reiniciado exitosamente en EC2." -ForegroundColor Green

# 6. Esperar a que /actuator/health responda 200 UP
Write-Host ""
Write-Host "Esperando que /actuator/health este disponible (UP)..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 1
$healthOk = $false

while ($attempt -le $maxAttempts) {
    $healthCmdId = (aws ssm send-command `
        --instance-ids "$instanceId" `
        --document-name "AWS-RunShellScript" `
        --parameters 'commands=["curl -s -f http://localhost:8080/actuator/health || echo DOWN"]' `
        --region "$Region" `
        --query "Command.CommandId" `
        --output text).Trim()

    Start-Sleep -Seconds 3
    $healthOutput = (aws ssm get-command-invocation `
        --command-id "$healthCmdId" `
        --instance-id "$instanceId" `
        --region "$Region" `
        --query "StandardOutputContent" `
        --output text 2>$null)

    if ($healthOutput -match '"status":"UP"') {
        Write-Host "-> [Intento $attempt/$maxAttempts] Backend activo: $healthOutput" -ForegroundColor Green
        $healthOk = $true
        break
    } else {
        Write-Host "-> [Intento $attempt/$maxAttempts] Esperando arranque de Spring Boot... (5s)" -ForegroundColor DarkGray
        Start-Sleep -Seconds 5
    }
    $attempt++
}

if ($healthOk) {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host " Publicacion de backend completada exitosamente!" -ForegroundColor Green
    Write-Host " Version desplegada: $commitSha" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Cyan
} else {
    Write-Error "Advertencia: Se supero el tiempo de espera. Revisa los logs con logs-backend.ps1."
    exit 1
}
