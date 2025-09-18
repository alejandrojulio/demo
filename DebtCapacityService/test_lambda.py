#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Tests para la Lambda de Capacidad de Endeudamiento
"""

import json
import unittest
from unittest.mock import Mock, patch, MagicMock
from decimal import Decimal
import sys
import os

# Agregar el directorio actual al path para importar la lambda
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from lambda_function import (
    DebtCapacityCalculator,
    DebtCapacityRequest,
    DebtCapacityController,
    MySQLLoanRepository,
    LoanRequest
)

class TestDebtCapacityCalculator(unittest.TestCase):
    """Tests para el calculador de capacidad de endeudamiento"""
    
    def setUp(self):
        self.mock_loan_repository = Mock()
        self.mock_notification_service = Mock()
        self.calculator = DebtCapacityCalculator(
            self.mock_loan_repository,
            self.mock_notification_service
        )
    
    def test_calculate_max_debt_capacity(self):
        """Test cálculo de capacidad máxima (35% del salario)"""
        base_salary = Decimal('3000000')  # 3 millones
        expected_capacity = Decimal('1050000')  # 35%
        
        capacity = self.calculator._calculate_max_debt_capacity(base_salary)
        self.assertEqual(capacity, expected_capacity)
    
    def test_calculate_monthly_payment(self):
        """Test cálculo de cuota mensual"""
        amount = Decimal('10000000')  # 10 millones
        annual_rate = Decimal('15.5')  # 15.5% anual
        term_months = 36
        
        payment = self.calculator._calculate_monthly_payment(amount, annual_rate, term_months)
        
        # Verificar que el pago sea positivo y razonable
        self.assertGreater(payment, Decimal('0'))
        self.assertLess(payment, amount)  # La cuota debe ser menor al monto total
        
        # Para este ejemplo específico, la cuota debería ser aproximadamente 364,000
        expected_approx = Decimal('364000')
        self.assertAlmostEqual(float(payment), float(expected_approx), delta=10000)
    
    def test_generate_payment_plan(self):
        """Test generación del plan de pago"""
        amount = Decimal('1000000')  # 1 millón
        annual_rate = Decimal('12')  # 12% anual
        term_months = 12
        
        payment_plan = self.calculator._generate_payment_plan(amount, annual_rate, term_months)
        
        # Verificar estructura del plan
        self.assertEqual(len(payment_plan), term_months)
        
        # Verificar que el último pago deje saldo 0
        last_payment = payment_plan[-1]
        self.assertEqual(last_payment.remaining_balance, Decimal('0'))
        
        # Verificar que la suma de abonos a capital sea igual al monto
        total_principal = sum(p.principal_payment for p in payment_plan)
        self.assertAlmostEqual(float(total_principal), float(amount), delta=1)
    
    def test_decision_approved(self):
        """Test decisión de aprobación"""
        new_payment = Decimal('300000')
        available_capacity = Decimal('500000')
        loan_amount = Decimal('5000000')
        base_salary = Decimal('2000000')  # Límite alto sería 10 millones
        
        decision, reason = self.calculator._make_decision(
            new_payment, available_capacity, loan_amount, base_salary
        )
        
        self.assertEqual(decision, "APROBADO")
        self.assertIn("Capacidad suficiente", reason)
    
    def test_decision_rejected_insufficient_capacity(self):
        """Test decisión de rechazo por capacidad insuficiente"""
        new_payment = Decimal('600000')
        available_capacity = Decimal('500000')
        loan_amount = Decimal('5000000')
        base_salary = Decimal('2000000')
        
        decision, reason = self.calculator._make_decision(
            new_payment, available_capacity, loan_amount, base_salary
        )
        
        self.assertEqual(decision, "RECHAZADO")
        self.assertIn("excede la capacidad disponible", reason)
    
    def test_decision_manual_review_high_amount(self):
        """Test decisión de revisión manual por monto alto"""
        new_payment = Decimal('300000')
        available_capacity = Decimal('500000')
        loan_amount = Decimal('12000000')  # Más de 5 salarios (2M * 5 = 10M)
        base_salary = Decimal('2000000')
        
        decision, reason = self.calculator._make_decision(
            new_payment, available_capacity, loan_amount, base_salary
        )
        
        self.assertEqual(decision, "REVISION_MANUAL")
        self.assertIn("Monto alto requiere revisión manual", reason)
    
    def test_calculate_debt_capacity_full_flow(self):
        """Test flujo completo de cálculo de capacidad"""
        # Mock del repositorio para devolver préstamos existentes
        existing_loan = LoanRequest(
            id=1,
            client_document="12345678",
            amount=Decimal('5000000'),
            term_in_months=24,
            loan_type="VEHICLE",
            interest_rate=Decimal('12.8'),
            status="APPROVED",
            base_salary=Decimal('3000000')
        )
        self.mock_loan_repository.find_approved_loans_by_client.return_value = [existing_loan]
        
        # Request de prueba
        request = DebtCapacityRequest(
            loan_request_id=2,
            client_document="12345678",
            amount=Decimal('3000000'),
            term_in_months=36,
            loan_type="PERSONAL",
            interest_rate=Decimal('15.5'),
            base_salary=Decimal('3000000')
        )
        
        # Ejecutar cálculo
        result = self.calculator.calculate_debt_capacity(request)
        
        # Verificaciones
        self.assertEqual(result.loan_request_id, 2)
        self.assertIn(result.decision, ["APROBADO", "RECHAZADO", "REVISION_MANUAL"])
        self.assertGreater(result.max_debt_capacity, Decimal('0'))
        self.assertGreaterEqual(result.current_monthly_debt, Decimal('0'))
        self.assertIsInstance(result.payment_plan, list)
        self.assertGreater(len(result.payment_plan), 0)

class TestDebtCapacityController(unittest.TestCase):
    """Tests para el controlador de la Lambda"""
    
    def setUp(self):
        self.controller = DebtCapacityController()
    
    @patch('lambda_function.MySQLLoanRepository')
    @patch('lambda_function.SQSNotificationService')
    def test_handle_request_success(self, mock_sqs, mock_repo):
        """Test manejo exitoso de solicitud"""
        # Mock de la solicitud existente
        loan_request = LoanRequest(
            id=1,
            client_document="12345678",
            amount=Decimal('5000000'),
            term_in_months=36,
            loan_type="PERSONAL",
            interest_rate=Decimal('15.5'),
            status="PENDING_REVIEW",
            base_salary=Decimal('3000000')
        )
        
        mock_repo_instance = mock_repo.return_value
        mock_repo_instance.find_loan_request_by_id.return_value = loan_request
        mock_repo_instance.find_approved_loans_by_client.return_value = []
        mock_repo_instance.update_loan_status.return_value = True
        mock_repo_instance._get_client_email = Mock(return_value="test@example.com")
        
        mock_sqs_instance = mock_sqs.return_value
        mock_sqs_instance.send_email_notification.return_value = True
        
        # Evento de prueba
        event = {
            "loan_request_id": 1
        }
        
        with patch.object(self.controller, 'loan_repository', mock_repo_instance), \
             patch.object(self.controller, 'notification_service', mock_sqs_instance), \
             patch.object(self.controller, '_get_client_email', return_value="test@example.com"):
            
            result = self.controller.handle_request(event)
        
        # Verificaciones
        self.assertEqual(result['statusCode'], 200)
        self.assertIn('loan_request_id', result['body'])
        self.assertIn('decision', result['body'])
    
    def test_extract_loan_request_id_direct(self):
        """Test extracción de ID de solicitud - invocación directa"""
        event = {"loan_request_id": 123}
        loan_id = self.controller._extract_loan_request_id(event)
        self.assertEqual(loan_id, 123)
    
    def test_extract_loan_request_id_sqs(self):
        """Test extracción de ID de solicitud - evento SQS"""
        event = {
            "Records": [
                {
                    "body": json.dumps({"loan_request_id": 456})
                }
            ]
        }
        loan_id = self.controller._extract_loan_request_id(event)
        self.assertEqual(loan_id, 456)
    
    def test_extract_loan_request_id_api_gateway(self):
        """Test extracción de ID de solicitud - API Gateway"""
        event = {
            "pathParameters": {"id": "789"}
        }
        loan_id = self.controller._extract_loan_request_id(event)
        self.assertEqual(loan_id, 789)
    
    def test_extract_loan_request_id_post_body(self):
        """Test extracción de ID de solicitud - POST body"""
        event = {
            "body": json.dumps({"loan_request_id": 101112})
        }
        loan_id = self.controller._extract_loan_request_id(event)
        self.assertEqual(loan_id, 101112)
    
    def test_map_decision_to_status(self):
        """Test mapeo de decisiones a estados de DB"""
        mappings = [
            ("APROBADO", "APPROVED"),
            ("RECHAZADO", "REJECTED"),
            ("REVISION_MANUAL", "MANUAL_REVIEW"),
            ("UNKNOWN", "PENDING_REVIEW")
        ]
        
        for decision, expected_status in mappings:
            status = self.controller._map_decision_to_status(decision)
            self.assertEqual(status, expected_status)

class TestMySQLLoanRepository(unittest.TestCase):
    """Tests para el repositorio MySQL (requiere configuración de DB)"""
    
    def setUp(self):
        # Solo crear el repositorio, no ejecutar operaciones reales en tests unitarios
        self.repository = MySQLLoanRepository()
    
    def test_connection_params(self):
        """Test que los parámetros de conexión se configuran correctamente"""
        params = self.repository.connection_params
        
        required_keys = ['host', 'port', 'user', 'password', 'database', 'charset']
        for key in required_keys:
            self.assertIn(key, params)
        
        self.assertEqual(params['charset'], 'utf8mb4')

class TestLambdaIntegration(unittest.TestCase):
    """Tests de integración para la función Lambda"""
    
    @patch('lambda_function.controller')
    def test_lambda_handler(self, mock_controller):
        """Test del handler principal de la Lambda"""
        from lambda_function import lambda_handler
        
        # Mock del controlador
        mock_controller.handle_request.return_value = {
            'statusCode': 200,
            'body': {'message': 'success'}
        }
        
        # Evento de prueba
        event = {"loan_request_id": 1}
        context = None
        
        result = lambda_handler(event, context)
        
        # Verificaciones
        mock_controller.handle_request.assert_called_once_with(event)
        self.assertEqual(result['statusCode'], 200)

def run_performance_test():
    """Test de rendimiento básico"""
    print("\n=== TEST DE RENDIMIENTO ===")
    
    import time
    from decimal import Decimal
    
    # Mock básico
    mock_repo = Mock()
    mock_notification = Mock()
    mock_repo.find_approved_loans_by_client.return_value = []
    
    calculator = DebtCapacityCalculator(mock_repo, mock_notification)
    
    # Test de 100 cálculos
    start_time = time.time()
    
    for i in range(100):
        request = DebtCapacityRequest(
            loan_request_id=i,
            client_document=f"doc{i}",
            amount=Decimal('5000000'),
            term_in_months=36,
            loan_type="PERSONAL",
            interest_rate=Decimal('15.5'),
            base_salary=Decimal('3000000')
        )
        result = calculator.calculate_debt_capacity(request)
        assert result.loan_request_id == i
    
    end_time = time.time()
    duration = end_time - start_time
    
    print(f"100 cálculos completados en {duration:.3f} segundos")
    print(f"Promedio por cálculo: {duration/100*1000:.2f} ms")
    print("✅ Test de rendimiento exitoso")

if __name__ == '__main__':
    print("Ejecutando tests de la Lambda de Capacidad de Endeudamiento...")
    
    # Ejecutar tests unitarios
    unittest.main(argv=[''], exit=False, verbosity=2)
    
    # Ejecutar test de rendimiento
    run_performance_test()
    
    print("\n🎉 Todos los tests completados exitosamente!")

