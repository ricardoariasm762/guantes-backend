#!/bin/bash
set -e

STACK_NAME="${STACK_NAME:-guantes-backend-stack}"
REGION="${AWS_REGION:-us-east-1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo "=========================================================="
echo " Publicacion de Imagen Backend a Amazon ECR y EC2"
echo " Stack:  $STACK_NAME"
echo " Region: $REGION"
echo "=========================================================="

# 1. Obtener salidas de CloudFormation (UriECR, IdInstancia, DnsPublicoEC2)
echo "Consultando datos de despliegue desde CloudFormation..."
OUTPUTS_JSON=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query "Stacks[0].Outputs" \
    --output json)

ECR_URI=$(echo "$OUTPUTS_JSON" | jq -r '.[] | select(.OutputKey=="UriECR") | .OutputValue')
INSTANCE_ID=$(echo "$OUTPUTS_JSON" | jq -r '.[] | select(.OutputKey=="IdInstancia") | .OutputValue')
EC2_DNS=$(echo "$OUTPUTS_JSON" | jq -r '.[] | select(.OutputKey=="DnsPublicoEC2") | .OutputValue')

if [ -z "$ECR_URI" ] || [ -z "$INSTANCE_ID" ]; then
    echo "Error: No se pudieron obtener UriECR o IdInstancia del stack $STACK_NAME."
    exit 1
fi

echo "-> Repositorio ECR: $ECR_URI"
echo "-> Instancia EC2:   $INSTANCE_ID"
echo "-> DNS Publico EC2: $EC2_DNS"

# 2. Obtener SHA corto del commit actual
COMMIT_SHA=$(git -C "$BACKEND_DIR" rev-parse --short HEAD)
echo "-> Version Commit:  $COMMIT_SHA"

# 3. Construir la imagen Docker para linux/amd64
echo ""
echo "Construyendo imagen Docker para linux/amd64..."
docker build \
    --platform linux/amd64 \
    -t "$ECR_URI:$COMMIT_SHA" \
    -t "$ECR_URI:latest" \
    "$BACKEND_DIR"

# 4. Iniciar sesion en Amazon ECR y publicar imagenes
echo ""
echo "Iniciando sesion en Amazon ECR..."
aws ecr get-login-password --region "$REGION" | \
    docker login --username AWS --password-stdin "$ECR_URI"

echo "Publicando imagen $ECR_URI:$COMMIT_SHA..."
docker push "$ECR_URI:$COMMIT_SHA"

echo "Publicando etiqueta $ECR_URI:latest..."
docker push "$ECR_URI:latest"

# 5. Reiniciar servicio en EC2 usando AWS SSM (sin SSH)
echo ""
echo "Reiniciando servicio guantes-backend en EC2 mediante AWS SSM..."
COMMAND_ID=$(aws ssm send-command \
    --instance-ids "$INSTANCE_ID" \
    --document-name "AWS-RunShellScript" \
    --parameters 'commands=["systemctl restart guantes-backend"]' \
    --comment "Reinicio por despliegue de imagen $COMMIT_SHA" \
    --region "$REGION" \
    --query "Command.CommandId" \
    --output text)

echo "-> Comando SSM enviado: $COMMAND_ID. Esperando ejecucion..."
aws ssm wait command-executed \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION"

echo "-> Servicio reiniciado exitosamente en EC2."

# 6. Esperar a que /actuator/health responda 200 UP
echo ""
echo "Esperando que /actuator/health este disponible (UP)..."
MAX_ATTEMPTS=30
ATTEMPT=1
HEALTH_OK=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    # Consultar estado de salud directamente dentro de la instancia mediante SSM
    STATUS=$(aws ssm send-command \
        --instance-ids "$INSTANCE_ID" \
        --document-name "AWS-RunShellScript" \
        --parameters 'commands=["curl -s -f http://localhost:8080/actuator/health || echo DOWN"]' \
        --region "$REGION" \
        --query "Command.CommandId" \
        --output text)
    
    sleep 3
    OUTPUT=$(aws ssm get-command-invocation \
        --command-id "$STATUS" \
        --instance-id "$INSTANCE_ID" \
        --region "$REGION" \
        --query "StandardOutputContent" \
        --output text 2>/dev/null || echo "")

    if echo "$OUTPUT" | grep -q '"status":"UP"'; then
        echo "-> [Intento $ATTEMPT/$MAX_ATTEMPTS] Backend activo: $OUTPUT"
        HEALTH_OK=true
        break
    else
        echo "-> [Intento $ATTEMPT/$MAX_ATTEMPTS] Esperando arranque de Spring Boot... (5s)"
        sleep 5
    fi
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$HEALTH_OK" = true ]; then
    echo ""
    echo "=========================================================="
    echo " Publicacion de backend completada exitosamente!"
    echo " Version desplegada: $COMMIT_SHA"
    echo "=========================================================="
else
    echo "Advertencia: Se supero el tiempo de espera. Revisa los logs con logs-backend.sh."
    exit 1
fi
