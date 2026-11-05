#!/bin/bash
set -e

STACK_NAME="${STACK_NAME:-guantes-backend-stack}"
REGION="${AWS_REGION:-us-east-1}"
LINES="${1:-100}"

echo "=========================================================="
echo " Consulta de Logs de Contenedor guantes-backend via SSM"
echo " Stack:  $STACK_NAME"
echo " Region: $REGION"
echo " Lineas: $LINES"
echo "=========================================================="

INSTANCE_ID=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='IdInstancia'].OutputValue" \
    --output text)

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
    echo "Error: No se pudo obtener el IdInstancia del stack $STACK_NAME."
    exit 1
fi

echo "Consultando logs en instancia $INSTANCE_ID..."

COMMAND_ID=$(aws ssm send-command \
    --instance-ids "$INSTANCE_ID" \
    --document-name "AWS-RunShellScript" \
    --parameters "commands=[\"docker logs --tail $LINES guantes-backend 2>&1 || journalctl -u guantes-backend -n $LINES --no-pager\"]" \
    --comment "Consulta de logs guantes-backend" \
    --region "$REGION" \
    --query "Command.CommandId" \
    --output text)

aws ssm wait command-executed \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION"

aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --region "$REGION" \
    --query "StandardOutputContent" \
    --output text
