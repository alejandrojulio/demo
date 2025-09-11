#!/bin/bash

# Script de despliegue para el servicio de notificaciones de CrediYa
# Utiliza AWS SAM para desplegar la Lambda, SQS y SNS

set -e

# Configuración
STACK_NAME="crediya-notification-service"
ENVIRONMENT=${1:-dev}
REGION=${AWS_REGION:-us-east-1}
SENDER_EMAIL=${2:-noreply@crediya.com}

echo "🚀 Desplegando CrediYa Notification Service..."
echo "   Entorno: $ENVIRONMENT"
echo "   Región: $REGION"
echo "   Stack: $STACK_NAME-$ENVIRONMENT"
echo "   Email remitente: $SENDER_EMAIL"
echo ""

# Verificar que AWS SAM CLI esté instalado
if ! command -v sam &> /dev/null; then
    echo "❌ AWS SAM CLI no está instalado. Por favor instala SAM CLI primero."
    exit 1
fi

# Verificar que AWS CLI esté configurado
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS CLI no está configurado. Por favor configura tus credenciales AWS."
    exit 1
fi

echo "✅ Prerequisitos verificados"

# Build del proyecto SAM
echo "🔨 Construyendo proyecto SAM..."
sam build

if [ $? -ne 0 ]; then
    echo "❌ Error en sam build"
    exit 1
fi

echo "✅ Build completado"

# Deploy del proyecto
echo "📦 Desplegando a AWS..."
sam deploy \
    --stack-name "$STACK_NAME-$ENVIRONMENT" \
    --parameter-overrides \
        Environment="$ENVIRONMENT" \
        SenderEmail="$SENDER_EMAIL" \
    --capabilities CAPABILITY_IAM \
    --region "$REGION" \
    --confirm-changeset \
    --resolve-s3

if [ $? -ne 0 ]; then
    echo "❌ Error en el despliegue"
    exit 1
fi

echo ""
echo "🎉 ¡Despliegue completado exitosamente!"
echo ""

# Obtener las URLs/ARNs importantes
echo "📋 Información del despliegue:"
echo "   Stack: $STACK_NAME-$ENVIRONMENT"

# Obtener SQS Queue URL
QUEUE_URL=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME-$ENVIRONMENT" \
    --region "$REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='LoanDecisionsQueueUrl'].OutputValue" \
    --output text)

echo "   SQS Queue URL: $QUEUE_URL"

# Obtener SNS Topic ARN
TOPIC_ARN=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME-$ENVIRONMENT" \
    --region "$REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='EmailNotificationTopicArn'].OutputValue" \
    --output text)

echo "   SNS Topic ARN: $TOPIC_ARN"

# Obtener Lambda Function ARN
FUNCTION_ARN=$(aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME-$ENVIRONMENT" \
    --region "$REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='NotificationFunctionArn'].OutputValue" \
    --output text)

echo "   Lambda Function ARN: $FUNCTION_ARN"

echo ""
echo "🔧 Configuración para el microservicio SOLICITUDES:"
echo "   aws.sqs.loan-decisions-queue-url: $QUEUE_URL"
echo ""
echo "⚠️  IMPORTANTE: Actualiza la configuración del microservicio SOLICITUDES con la Queue URL generada."
echo ""
echo "📧 Para configurar el envío de emails, suscribe un endpoint de email al tópico SNS:"
echo "   aws sns subscribe --topic-arn $TOPIC_ARN --protocol email --notification-endpoint tu-email@ejemplo.com"
echo ""
