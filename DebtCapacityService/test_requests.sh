#!/bin/bash

# Script para probar la Lambda de Capacidad de Endeudamiento con cURL
# Requiere que la Lambda esté desplegada en AWS

# Configuración
API_GATEWAY_URL="https://[TU-API-ID].execute-api.us-east-2.amazonaws.com/v1"
REGION="us-east-2"
LAMBDA_FUNCTION_NAME="crediya-debt-capacity-debt-capacity"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Testing Lambda Capacidad Endeudamiento${NC}"
echo -e "${GREEN}========================================${NC}"

# Verificar que AWS CLI esté instalado
if ! command -v aws &> /dev/null; then
    echo -e "${RED}AWS CLI no está instalado${NC}"
    exit 1
fi

# Test 1: Invocación directa de Lambda
echo -e "\n${BLUE}1. Testing Invocación Directa de Lambda${NC}"
echo "Request:"
cat << EOF
{
  "loan_request_id": 1
}
EOF

echo -e "\n${YELLOW}Ejecutando...${NC}"
aws lambda invoke \
    --function-name $LAMBDA_FUNCTION_NAME \
    --region $REGION \
    --payload '{"loan_request_id": 1}' \
    --cli-binary-format raw-in-base64-out \
    response1.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Invocación directa exitosa${NC}"
    echo "Respuesta:"
    cat response1.json | jq .
else
    echo -e "${RED}❌ Error en invocación directa${NC}"
fi

# Test 2: Request vía API Gateway
echo -e "\n${BLUE}2. Testing Request vía API Gateway${NC}"
echo "Request:"
echo "POST $API_GATEWAY_URL/api/v1/calcular-capacidad"

API_REQUEST='{
  "loan_request_id": 2
}'

echo "$API_REQUEST"

echo -e "\n${YELLOW}Ejecutando...${NC}"
curl -X POST "$API_GATEWAY_URL/api/v1/calcular-capacidad" \
     -H "Content-Type: application/json" \
     -d "$API_REQUEST" \
     -w "\nHTTP Status: %{http_code}\n" \
     -o response2.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Request API Gateway exitoso${NC}"
    echo "Respuesta:"
    cat response2.json | jq .
else
    echo -e "${RED}❌ Error en request API Gateway${NC}"
fi

# Test 3: Request con diferentes loan_request_id
echo -e "\n${BLUE}3. Testing Múltiples Solicitudes${NC}"

LOAN_IDS=(1 2 3 4 5)

for id in "${LOAN_IDS[@]}"; do
    echo -e "\n${YELLOW}Probando solicitud ID: $id${NC}"
    
    REQUEST_PAYLOAD="{\"loan_request_id\": $id}"
    
    aws lambda invoke \
        --function-name $LAMBDA_FUNCTION_NAME \
        --region $REGION \
        --payload "$REQUEST_PAYLOAD" \
        --cli-binary-format raw-in-base64-out \
        "response_$id.json" > /dev/null 2>&1
    
    if [ $? -eq 0 ]; then
        # Extraer información clave de la respuesta
        STATUS_CODE=$(cat "response_$id.json" | jq -r '.statusCode // "N/A"')
        DECISION=$(cat "response_$id.json" | jq -r '.body.decision // "N/A"')
        REASON=$(cat "response_$id.json" | jq -r '.body.reason // "N/A"')
        
        echo "   Status: $STATUS_CODE | Decisión: $DECISION"
        echo "   Razón: $REASON"
        
        if [ "$STATUS_CODE" = "200" ]; then
            echo -e "   ${GREEN}✅ Exitoso${NC}"
        else
            echo -e "   ${RED}❌ Error${NC}"
        fi
    else
        echo -e "   ${RED}❌ Error en invocación${NC}"
    fi
done

# Test 4: Request con solicitud inexistente
echo -e "\n${BLUE}4. Testing Solicitud Inexistente${NC}"
echo "Request con loan_request_id: 999 (inexistente)"

aws lambda invoke \
    --function-name $LAMBDA_FUNCTION_NAME \
    --region $REGION \
    --payload '{"loan_request_id": 999}' \
    --cli-binary-format raw-in-base64-out \
    response_error.json

echo "Respuesta:"
cat response_error.json | jq .

# Test 5: Request malformado
echo -e "\n${BLUE}5. Testing Request Malformado${NC}"
echo "Request sin loan_request_id:"

aws lambda invoke \
    --function-name $LAMBDA_FUNCTION_NAME \
    --region $REGION \
    --payload '{"invalid_field": "test"}' \
    --cli-binary-format raw-in-base64-out \
    response_malformed.json

echo "Respuesta:"
cat response_malformed.json | jq .

# Test 6: Test de carga (múltiples requests simultáneos)
echo -e "\n${BLUE}6. Testing Carga (10 requests simultáneos)${NC}"

echo "Ejecutando 10 requests en paralelo..."

for i in {1..10}; do
    {
        aws lambda invoke \
            --function-name $LAMBDA_FUNCTION_NAME \
            --region $REGION \
            --payload "{\"loan_request_id\": $((i % 5 + 1))}" \
            --cli-binary-format raw-in-base64-out \
            "load_test_$i.json" > /dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            echo "Request $i: ✅"
        else
            echo "Request $i: ❌"
        fi
    } &
done

# Esperar a que todos los jobs terminen
wait

echo -e "${GREEN}Test de carga completado${NC}"

# Limpiar archivos de respuesta
echo -e "\n${YELLOW}Limpiando archivos temporales...${NC}"
rm -f response*.json load_test_*.json

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}   Tests Completados${NC}"
echo -e "${GREEN}========================================${NC}"

echo -e "\n${BLUE}Comandos útiles:${NC}"
echo "# Ver logs de la Lambda:"
echo "aws logs tail /aws/lambda/$LAMBDA_FUNCTION_NAME --follow"
echo ""
echo "# Invocar con payload personalizado:"
echo "aws lambda invoke --function-name $LAMBDA_FUNCTION_NAME --payload '{\"loan_request_id\": 1}' response.json"
echo ""
echo "# Test vía API Gateway:"
echo "curl -X POST '$API_GATEWAY_URL/api/v1/calcular-capacidad' -H 'Content-Type: application/json' -d '{\"loan_request_id\": 1}'"

