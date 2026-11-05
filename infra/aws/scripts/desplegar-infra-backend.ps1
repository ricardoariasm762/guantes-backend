param (
    [string]$StackName = "guantes-backend-stack",
    [string]$Region = "us-east-2",
    [string]$VpcId = "",
    [string]$SubnetIds = ""
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$templateFile = Join-Path (Split-Path -Parent $scriptDir) "backend.yaml"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Despliegue de Infraestructura Backend con CloudFormation" -ForegroundColor Cyan
Write-Host " Stack:     $StackName" -ForegroundColor Cyan
Write-Host " Region:    $Region" -ForegroundColor Cyan
Write-Host " Plantilla: $templateFile" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Obtener ID de la lista de prefijos administrada de CloudFront Origin-Facing
Write-Host "Consultando ID de prefijo com.amazonaws.global.cloudfront.origin-facing..." -ForegroundColor Yellow
$prefixListId = (aws ec2 describe-managed-prefix-lists `
    --filters "Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing" `
    --query "PrefixLists[0].PrefixListId" `
    --output text `
    --region "$Region").Trim()

if ([string]::IsNullOrWhiteSpace($prefixListId) -or $prefixListId -eq "None") {
    Write-Error "Error: No se pudo encontrar la lista de prefijos de CloudFront en la region $Region."
    exit 1
}
Write-Host "-> Prefix List ID: $prefixListId" -ForegroundColor Green

# 2. Obtener VPC por defecto si no se especifico
if ([string]::IsNullOrWhiteSpace($VpcId)) {
    Write-Host "Consultando VPC por defecto..." -ForegroundColor Yellow
    $VpcId = (aws ec2 describe-vpcs `
        --filters "Name=isDefault,Values=true" `
        --query "Vpcs[0].VpcId" `
        --output text `
        --region "$Region").Trim()
}
Write-Host "-> VPC ID: $VpcId" -ForegroundColor Green

# 3. Obtener al menos 2 subredes publicas en zonas distintas
if ([string]::IsNullOrWhiteSpace($SubnetIds)) {
    Write-Host "Consultando subredes de la VPC $VpcId..." -ForegroundColor Yellow
    $subnetsRaw = aws ec2 describe-subnets `
        --filters "Name=vpc-id,Values=$VpcId" `
        --query "Subnets[0:2].SubnetId" `
        --output text `
        --region "$Region"
    $SubnetIds = ($subnetsRaw -split "`t" -join ",").Trim()
}
Write-Host "-> Subnet IDs: $SubnetIds" -ForegroundColor Green

# 4. Validar plantilla con cfn-lint si esta disponible
if (Get-Command cfn-lint -ErrorAction SilentlyContinue) {
    Write-Host "Validando plantilla con cfn-lint..." -ForegroundColor Yellow
    cfn-lint "$templateFile"
    Write-Host "-> Plantilla valida." -ForegroundColor Green
}

# 5. Desplegar con CloudFormation
Write-Host "Iniciando despliegue de CloudFormation..." -ForegroundColor Yellow
aws cloudformation deploy `
    --template-file "$templateFile" `
    --stack-name "$StackName" `
    --capabilities CAPABILITY_NAMED_IAM `
    --parameter-overrides `
        VpcId="$VpcId" `
        SubnetIds="$SubnetIds" `
        CloudFrontPrefixListId="$prefixListId" `
    --region "$Region"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Despliegue de infraestructura completado exitosamente!" -ForegroundColor Green
Write-Host " Salidas del Stack:" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
aws cloudformation describe-stacks `
    --stack-name "$StackName" `
    --region "$Region" `
    --query "Stacks[0].Outputs" `
    --output table
