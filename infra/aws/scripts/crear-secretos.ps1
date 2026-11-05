param (
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Creacion interactiva de Secretos en AWS SSM Parameter Store" -ForegroundColor Cyan
Write-Host " Region: $Region" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. JWT_SECRET
$jwtInput = Read-Host "Introduce JWT_SECRET (Enter para generar uno aleatorio seguro de 48 bytes)" -AsSecureString
$jwtBstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($jwtInput)
$jwtPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($jwtBstr)

if ([string]::IsNullOrWhiteSpace($jwtPlain)) {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $jwtPlain = [Convert]::ToBase64String($bytes)
    Write-Host "-> Generado JWT_SECRET aleatorio de 48 bytes." -ForegroundColor Green
} elseif ($jwtPlain.Length -lt 32) {
    Write-Error "Error: JWT_SECRET debe tener al menos 32 caracteres (256 bits)."
    exit 1
}

# 2. DB_PASSWORD
$dbInput = Read-Host "Introduce contrasena maestra para PostgreSQL RDS (DB_PASSWORD)" -AsSecureString
$dbBstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbInput)
$dbPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($dbBstr)

if ([string]::IsNullOrWhiteSpace($dbPlain)) {
    Write-Error "Error: DB_PASSWORD no puede estar vacio."
    exit 1
}

# 3. ADMIN_PASSWORD
$adminInput = Read-Host "Introduce contrasena del usuario ADMIN inicial (ADMIN_PASSWORD)" -AsSecureString
$adminBstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($adminInput)
$adminPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($adminBstr)

if ([string]::IsNullOrWhiteSpace($adminPlain)) {
    Write-Error "Error: ADMIN_PASSWORD no puede estar vacio."
    exit 1
}

# 4. ADMIN_EMAIL
$adminEmail = Read-Host "Introduce correo del usuario ADMIN inicial [admin@ucc.edu.co]"
if ([string]::IsNullOrWhiteSpace($adminEmail)) {
    $adminEmail = "admin@ucc.edu.co"
}

# 5. CORS_ALLOWED_ORIGINS
$corsOrigin = Read-Host "Introduce origen CORS permitido (ej. https://d1234.cloudfront.net) [http://localhost:5173]"
if ([string]::IsNullOrWhiteSpace($corsOrigin)) {
    $corsOrigin = "http://localhost:5173"
}

Write-Host ""
Write-Host "Almacenando parametros en AWS SSM Parameter Store..." -ForegroundColor Yellow

aws ssm put-parameter `
    --name "/guantes/JWT_SECRET" `
    --value "$jwtPlain" `
    --type "SecureString" `
    --overwrite `
    --region "$Region" | Out-Null

aws ssm put-parameter `
    --name "/guantes/DB_PASSWORD" `
    --value "$dbPlain" `
    --type "SecureString" `
    --overwrite `
    --region "$Region" | Out-Null

aws ssm put-parameter `
    --name "/guantes/ADMIN_PASSWORD" `
    --value "$adminPlain" `
    --type "SecureString" `
    --overwrite `
    --region "$Region" | Out-Null

aws ssm put-parameter `
    --name "/guantes/ADMIN_EMAIL" `
    --value "$adminEmail" `
    --type "String" `
    --overwrite `
    --region "$Region" | Out-Null

aws ssm put-parameter `
    --name "/guantes/CORS_ALLOWED_ORIGINS" `
    --value "$corsOrigin" `
    --type "String" `
    --overwrite `
    --region "$Region" | Out-Null

Write-Host "Parametros guardados exitosamente bajo el prefijo /guantes/* en la region $Region." -ForegroundColor Green
