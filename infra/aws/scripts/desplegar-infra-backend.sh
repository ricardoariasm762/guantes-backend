#!/bin/bash
set -e

STACK_NAME="${STACK_NAME:-guantes-backend-stack}"
REGION="${AWS_REGION:-us-east-1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_FILE="$(cd "$SCRIPT_DIR/.." && pwd)/backend.yaml"

echo "=========================================================="
echo " Despliegue de Infraestructura Backend con CloudFormation"
echo " Stack:   $STACK_NAME"
echo " Region:  $REGION"
echo " Plantilla: $TEMPLATE_FILE"
echo "=========================================================="

# 1. Obtener ID de la lista de prefijos administrada de CloudFront Origin-Facing
echo "Consultando ID de prefijo com.amazonaws.global.cloudfront.origin-facing..."
PREFIX_LIST_ID=$(aws ec2 describe-managed-prefix-lists \
    --filters "Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing" \
    --query "PrefixLists[0].PrefixListId" \
    --output text \
    --region "$REGION")

if [ -z "$PREFIX_LIST_ID" ] || [ "$PREFIX_LIST_ID" = "None" ]; then
    echo "Error: No se pudo encontrar la lista de prefijos de CloudFront en la region $REGION."
    exit 1
fi
echo "-> Prefix List ID: $PREFIX_LIST_ID"

# 2. Obtener VPC por defecto si no se especifico en el entorno
if [ -z "$VPC_ID" ]; then
    echo "Consultando VPC por defecto..."
    VPC_ID=$(aws ec2 describe-vpcs \
        --filters "Name=isDefault,Values=true" \
        --query "Vpcs[0].VpcId" \
        --output text \
        --region "$REGION")
fi
echo "-> VPC ID: $VPC_ID"

# 3. Obtener al menos 2 subredes publicas en zonas distintas
if [ -z "$SUBNET_IDS" ]; then
    echo "Consultando subredes de la VPC $VPC_ID..."
    SUBNET_IDS=$(aws ec2 describe-subnets \
        --filters "Name=vpc-id,Values=$VPC_ID" \
        --query "Subnets[0:2].SubnetId" \
        --output text \
        --region "$REGION" | tr '\t' ',')
fi
echo "-> Subnet IDs: $SUBNET_IDS"

# 4. Validar plantilla con cfn-lint si esta disponible
if command -v cfn-lint &> /dev/null; then
    echo "Validando plantilla con cfn-lint..."
    cfn-lint "$TEMPLATE_FILE"
    echo "-> Plantilla valida."
fi

# 5. Desplegar con CloudFormation
echo "Iniciando despliegue de CloudFormation..."
aws cloudformation deploy \
    --template-file "$TEMPLATE_FILE" \
    --stack-name "$STACK_NAME" \
    --capabilities CAPABILITY_NAMED_IAM \
    --parameter-overrides \
        VpcId="$VPC_ID" \
        SubnetIds="$SUBNET_IDS" \
        CloudFrontPrefixListId="$PREFIX_LIST_ID" \
    --region "$REGION"

echo ""
echo "=========================================================="
echo " Despliegue de infraestructura completado exitosamente!"
echo " Salidas del Stack:"
echo "=========================================================="
aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query "Stacks[0].Outputs" \
    --output table
