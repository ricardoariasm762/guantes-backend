#!/bin/bash
set -e

REGION="${AWS_REGION:-us-east-2}"
echo "=========================================================="
echo " Creacion interactiva de Secretos en AWS SSM Parameter Store"
echo " Region: $REGION"
echo "=========================================================="
echo ""

# 1. JWT_SECRET
read -s -p "Introduce JWT_SECRET (Enter para generar uno aleatorio seguro de 48 bytes): " JWT_SECRET
echo ""
if [ -z "$JWT_SECRET" ]; then
    JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
    echo "-> Generado JWT_SECRET aleatorio de 48 bytes."
else
    if [ ${#JWT_SECRET} -lt 32 ]; then
        echo "Error: JWT_SECRET debe tener al menos 32 caracteres (256 bits)."
        exit 1
    fi
fi

# 2. DB_PASSWORD
read -s -p "Introduce contrasena maestra para PostgreSQL RDS (DB_PASSWORD): " DB_PASSWORD
echo ""
if [ -z "$DB_PASSWORD" ]; then
    echo "Error: DB_PASSWORD no puede estar vacio."
    exit 1
fi

# 3. ADMIN_PASSWORD
read -s -p "Introduce contrasena del usuario ADMIN inicial (ADMIN_PASSWORD): " ADMIN_PASSWORD
echo ""
if [ -z "$ADMIN_PASSWORD" ]; then
    echo "Error: ADMIN_PASSWORD no puede estar vacio."
    exit 1
fi

# 4. ADMIN_EMAIL
read -p "Introduce correo del usuario ADMIN inicial [admin@ucc.edu.co]: " ADMIN_EMAIL
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@ucc.edu.co}"

# 5. CORS_ALLOWED_ORIGINS
read -p "Introduce origen CORS permitido (ej. https://d1234.cloudfront.net) [http://localhost:5173]: " CORS_ALLOWED_ORIGINS
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:5173}"

echo ""
echo "Almacenando parametros en AWS SSM Parameter Store..."

aws ssm put-parameter \
    --name "/guantes/JWT_SECRET" \
    --value "$JWT_SECRET" \
    --type "SecureString" \
    --overwrite \
    --region "$REGION" > /dev/null

aws ssm put-parameter \
    --name "/guantes/DB_PASSWORD" \
    --value "$DB_PASSWORD" \
    --type "SecureString" \
    --overwrite \
    --region "$REGION" > /dev/null

aws ssm put-parameter \
    --name "/guantes/ADMIN_PASSWORD" \
    --value "$ADMIN_PASSWORD" \
    --type "SecureString" \
    --overwrite \
    --region "$REGION" > /dev/null

aws ssm put-parameter \
    --name "/guantes/ADMIN_EMAIL" \
    --value "$ADMIN_EMAIL" \
    --type "String" \
    --overwrite \
    --region "$REGION" > /dev/null

aws ssm put-parameter \
    --name "/guantes/CORS_ALLOWED_ORIGINS" \
    --value "$CORS_ALLOWED_ORIGINS" \
    --type "String" \
    --overwrite \
    --region "$REGION" > /dev/null

echo "Parametros guardados exitosamente bajo el prefijo /guantes/* en la region $REGION."
