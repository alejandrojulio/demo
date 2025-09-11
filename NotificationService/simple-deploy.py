#!/usr/bin/env python3
"""
Script simple para crear la Lambda de notificaciones sin SAM
Solo crea la función Lambda básica - SQS y SNS deben existir previamente
"""

import boto3
import zipfile
import os
import json

def create_lambda_package():
    """Crea el ZIP con el código de la Lambda"""
    with zipfile.ZipFile('notification-lambda.zip', 'w') as zip_file:
        zip_file.write('lambda_function.py')
        # Agregar dependencias si las hay

def deploy_lambda():
    """Despliega la Lambda usando boto3"""
    lambda_client = boto3.client('lambda')
    
    # Leer el ZIP
    with open('notification-lambda.zip', 'rb') as zip_file:
        zip_content = zip_file.read()
    
    try:
        # Crear la función Lambda
        response = lambda_client.create_function(
            FunctionName='crediya-notification-service',
            Runtime='python3.9',
            Role='arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role',  # Debe existir
            Handler='lambda_function.lambda_handler',
            Code={'ZipFile': zip_content},
            Environment={
                'Variables': {
                    'SNS_TOPIC_ARN': 'arn:aws:sns:us-east-1:YOUR_ACCOUNT:email-notifications'  # Debe existir
                }
            },
            Timeout=30,
            MemorySize=256
        )
        print(f"✅ Lambda creada: {response['FunctionArn']}")
        
    except lambda_client.exceptions.ResourceConflictException:
        # Si ya existe, actualizar
        response = lambda_client.update_function_code(
            FunctionName='crediya-notification-service',
            ZipFile=zip_content
        )
        print(f"✅ Lambda actualizada: {response['FunctionArn']}")

if __name__ == "__main__":
    print("🚀 Desplegando Lambda de notificaciones...")
    create_lambda_package()
    deploy_lambda()
    os.remove('notification-lambda.zip')
    print("✅ Despliegue completado")
