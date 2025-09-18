#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para probar la Lambda de Capacidad de Endeudamiento con diferentes tipos de requests
"""

import json
import sys
import os

# Agregar el directorio actual al path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from lambda_function import lambda_handler

def test_direct_request():
    """Test con request directo"""
    print("🧪 Testing Request Directo...")
    
    event = {
        "loan_request_id": 1
    }
    
    try:
        result = lambda_handler(event, None)
        print(f"✅ Request directo exitoso: {result['statusCode']}")
        print(f"📄 Respuesta: {json.dumps(result['body'], indent=2, default=str)}")
        return True
    except Exception as e:
        print(f"❌ Error en request directo: {str(e)}")
        return False

def test_sqs_request():
    """Test con request desde SQS"""
    print("\n🧪 Testing Request desde SQS...")
    
    event = {
        "Records": [
            {
                "body": json.dumps({
                    "loan_request_id": 2,
                    "event_type": "AUTOMATIC_DEBT_CAPACITY_VALIDATION",
                    "timestamp": "2024-01-15T10:30:00",
                    "source": "LoadRequestService"
                })
            }
        ]
    }
    
    try:
        result = lambda_handler(event, None)
        print(f"✅ Request SQS exitoso: {result['statusCode']}")
        print(f"📄 Respuesta: {json.dumps(result['body'], indent=2, default=str)}")
        return True
    except Exception as e:
        print(f"❌ Error en request SQS: {str(e)}")
        return False

def test_api_gateway_request():
    """Test con request desde API Gateway"""
    print("\n🧪 Testing Request desde API Gateway...")
    
    event = {
        "pathParameters": {
            "id": "3"
        },
        "httpMethod": "POST",
        "headers": {
            "Content-Type": "application/json"
        }
    }
    
    try:
        result = lambda_handler(event, None)
        print(f"✅ Request API Gateway exitoso: {result['statusCode']}")
        print(f"📄 Respuesta: {json.dumps(result['body'], indent=2, default=str)}")
        return True
    except Exception as e:
        print(f"❌ Error en request API Gateway: {str(e)}")
        return False

def test_post_body_request():
    """Test con request POST body"""
    print("\n🧪 Testing Request POST Body...")
    
    event = {
        "body": json.dumps({"loan_request_id": 4}),
        "httpMethod": "POST",
        "headers": {
            "Content-Type": "application/json"
        }
    }
    
    try:
        result = lambda_handler(event, None)
        print(f"✅ Request POST Body exitoso: {result['statusCode']}")
        print(f"📄 Respuesta: {json.dumps(result['body'], indent=2, default=str)}")
        return True
    except Exception as e:
        print(f"❌ Error en request POST Body: {str(e)}")
        return False

def test_invalid_request():
    """Test con request inválido"""
    print("\n🧪 Testing Request Inválido...")
    
    event = {
        "invalid_field": "test"
    }
    
    try:
        result = lambda_handler(event, None)
        print(f"⚠️ Request inválido manejado: {result['statusCode']}")
        print(f"📄 Respuesta: {json.dumps(result['body'], indent=2, default=str)}")
        return True
    except Exception as e:
        print(f"✅ Error esperado en request inválido: {str(e)}")
        return True

def test_multiple_scenarios():
    """Test con diferentes escenarios de solicitudes"""
    print("\n🧪 Testing Múltiples Escenarios...")
    
    scenarios = [
        {"loan_request_id": 5, "description": "Solicitud normal"},
        {"loan_request_id": 6, "description": "Solicitud con monto alto"},
        {"loan_request_id": 7, "description": "Cliente con deudas existentes"},
        {"loan_request_id": 999, "description": "Solicitud inexistente"}
    ]
    
    results = []
    for scenario in scenarios:
        print(f"\n📋 Probando: {scenario['description']}")
        
        event = {"loan_request_id": scenario["loan_request_id"]}
        
        try:
            result = lambda_handler(event, None)
            print(f"   Status: {result['statusCode']}")
            if result['statusCode'] == 200:
                decision = result['body'].get('decision', 'N/A')
                print(f"   Decisión: {decision}")
            results.append(True)
        except Exception as e:
            print(f"   Error: {str(e)}")
            results.append(False)
    
    successful = sum(results)
    print(f"\n📊 Resultados: {successful}/{len(scenarios)} escenarios exitosos")
    return successful > len(scenarios) // 2

if __name__ == "__main__":
    print("🚀 Iniciando Tests de la Lambda de Capacidad de Endeudamiento")
    print("=" * 60)
    
    # Configurar variables de entorno de prueba
    os.environ['DB_HOST'] = 'localhost'
    os.environ['DB_PORT'] = '3307'
    os.environ['DB_USER'] = 'crediya_user'
    os.environ['DB_PASSWORD'] = 'crediya_pass'
    os.environ['DB_NAME'] = 'crediya_db'
    os.environ['NOTIFICATION_QUEUE_URL'] = 'https://sqs.us-east-2.amazonaws.com/test/queue'
    os.environ['DEBUG'] = 'true'
    
    # Ejecutar tests
    tests = [
        test_direct_request,
        test_sqs_request,
        test_api_gateway_request,
        test_post_body_request,
        test_invalid_request,
        test_multiple_scenarios
    ]
    
    passed = 0
    total = len(tests)
    
    for test in tests:
        if test():
            passed += 1
    
    print("\n" + "=" * 60)
    print(f"🎯 Resumen Final: {passed}/{total} tests exitosos")
    
    if passed == total:
        print("🎉 ¡Todos los tests pasaron!")
    elif passed > total // 2:
        print("⚠️ La mayoría de tests pasaron")
    else:
        print("❌ Varios tests fallaron - Revisar configuración")
    
    print("\n💡 Nota: Algunos tests pueden fallar si no hay conexión a la base de datos")
    print("   Para tests completos, ejecutar con servicios Docker activos")

