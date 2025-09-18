# -*- coding: utf-8 -*-
"""
Lambda de Capacidad de Endeudamiento - CrediYa
Implementa el cálculo automático de capacidad de endeudamiento siguiendo arquitectura hexagonal.
"""

import json
import logging
import os
import traceback
import time
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, asdict
from decimal import Decimal, ROUND_HALF_UP
from datetime import datetime
import boto3
from botocore.exceptions import ClientError
# urllib ya no necesario - usando solo SQS

# Configuración de logs
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ========================================
# DOMINIO - ENTIDADES Y VALUE OBJECTS
# ========================================

@dataclass
class DebtCapacityRequest:
    """Request para cálculo de capacidad de endeudamiento"""
    loan_request_id: int
    client_document: str
    amount: Decimal
    term_in_months: int
    loan_type: str
    interest_rate: Decimal
    base_salary: Decimal
    client_email: str
    client_name: str
    approved_loans: List[dict]

@dataclass
class PaymentPlan:
    """Plan de pago individual"""
    period: int
    principal_payment: Decimal
    interest_payment: Decimal
    total_payment: Decimal
    remaining_balance: Decimal

@dataclass
class DebtCapacityResult:
    """Resultado del cálculo de capacidad de endeudamiento"""
    loan_request_id: int
    decision: str  # APROBADO, RECHAZADO, REVISION_MANUAL
    max_debt_capacity: Decimal
    current_monthly_debt: Decimal
    available_capacity: Decimal
    new_loan_payment: Decimal
    reason: str
    payment_plan: List[PaymentPlan]

# ========================================
# PUERTOS DEL DOMINIO
# ========================================

class NotificationService:
    """Puerto para envío de notificaciones"""
    
    def send_email_notification(self, loan_request_id: int, client_email: str, 
                               decision: str, payment_plan: List[PaymentPlan]) -> bool:
        raise NotImplementedError

class StatusUpdateService:
    """Puerto para actualización de estado de solicitudes"""
    
    def update_loan_status(self, loan_id: int, status: str, reason: str) -> bool:
        raise NotImplementedError

# ========================================
# CASOS DE USO DEL DOMINIO
# ========================================

class DebtCapacityCalculator:
    """Caso de uso principal para cálculo de capacidad de endeudamiento"""
    
    # Constantes de negocio
    MAX_DEBT_RATIO = Decimal('0.35')  # 35% máximo
    HIGH_AMOUNT_THRESHOLD_MULTIPLIER = 5  # 5 veces el salario
    
    def __init__(self, notification_service: NotificationService, status_update_service: StatusUpdateService):
        self.notification_service = notification_service
        self.status_update_service = status_update_service
    
    def calculate_debt_capacity(self, request: DebtCapacityRequest) -> DebtCapacityResult:
        """Calcula la capacidad de endeudamiento y toma la decisión"""
        logger.info(f"Iniciando cálculo de capacidad para solicitud {request.loan_request_id}")
        
        try:
            # 1. Calcular capacidad máxima de endeudamiento (35% del salario)
            max_capacity = self._calculate_max_debt_capacity(request.base_salary)
            
            # 2. Obtener deuda actual del cliente
            current_debt = self._calculate_current_monthly_debt(request.approved_loans)
            
            # 3. Calcular capacidad disponible
            available_capacity = max_capacity - current_debt
            
            # 4. Calcular cuota del nuevo préstamo
            new_loan_payment = self._calculate_monthly_payment(
                request.amount, request.interest_rate, request.term_in_months
            )
            
            # 5. Generar plan de pago
            payment_plan = self._generate_payment_plan(
                request.amount, request.interest_rate, request.term_in_months
            )
            
            # 6. Tomar decisión
            decision, reason = self._make_decision(
                new_loan_payment, available_capacity, request.amount, request.base_salary
            )
            
            result = DebtCapacityResult(
                loan_request_id=request.loan_request_id,
                decision=decision,
                max_debt_capacity=max_capacity,
                current_monthly_debt=current_debt,
                available_capacity=available_capacity,
                new_loan_payment=new_loan_payment,
                reason=reason,
                payment_plan=payment_plan
            )
            
            logger.info(f"Cálculo completado para solicitud {request.loan_request_id}: {decision}")
            return result
            
        except Exception as e:
            logger.error(f"Error calculando capacidad para solicitud {request.loan_request_id}: {str(e)}")
            logger.error(traceback.format_exc())
            raise
    
    def _calculate_max_debt_capacity(self, base_salary: Decimal) -> Decimal:
        """Calcula la capacidad máxima de endeudamiento (35% del salario)"""
        return base_salary * self.MAX_DEBT_RATIO
    
    def _calculate_current_monthly_debt(self, approved_loans: List[dict]) -> Decimal:
        """Calcula la deuda mensual actual del cliente desde los datos enviados"""
        total_debt = Decimal('0')
        
        for loan_data in approved_loans:
            amount = Decimal(str(loan_data.get('amount', 0)))
            interest_rate = Decimal(str(loan_data.get('interest_rate', 0)))
            term_months = int(loan_data.get('term_in_months', 1))
            
            monthly_payment = self._calculate_monthly_payment(amount, interest_rate, term_months)
            total_debt += monthly_payment
            
        logger.info(f"Deuda mensual actual calculada desde {len(approved_loans)} préstamos: ${total_debt}")
        return total_debt
    
    def _calculate_monthly_payment(self, amount: Decimal, annual_rate: Decimal, term_months: int) -> Decimal:
        """Calcula la cuota mensual usando la fórmula de amortización"""
        if annual_rate == 0:
            return amount / term_months
            
        monthly_rate = annual_rate / Decimal('100') / Decimal('12')
        numerator = amount * monthly_rate * ((1 + monthly_rate) ** term_months)
        denominator = ((1 + monthly_rate) ** term_months) - 1
        
        payment = numerator / denominator
        return payment.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
    
    def _generate_payment_plan(self, amount: Decimal, annual_rate: Decimal, term_months: int) -> List[PaymentPlan]:
        """Genera el plan de pago completo"""
        payment_plan = []
        remaining_balance = amount
        monthly_payment = self._calculate_monthly_payment(amount, annual_rate, term_months)
        monthly_rate = annual_rate / Decimal('100') / Decimal('12')
        
        for period in range(1, term_months + 1):
            interest_payment = remaining_balance * monthly_rate
            principal_payment = monthly_payment - interest_payment
            remaining_balance -= principal_payment
            
            # Ajustar último pago para que el saldo quede en 0
            if period == term_months:
                principal_payment += remaining_balance
                remaining_balance = Decimal('0')
            
            payment_plan.append(PaymentPlan(
                period=period,
                principal_payment=principal_payment.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP),
                interest_payment=interest_payment.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP),
                total_payment=monthly_payment.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP),
                remaining_balance=remaining_balance.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
            ))
        
        return payment_plan
    
    def _make_decision(self, new_payment: Decimal, available_capacity: Decimal, 
                      loan_amount: Decimal, base_salary: Decimal) -> tuple[str, str]:
        """Toma la decisión de aprobación/rechazo"""
        
        # Verificar si el cliente puede pagar la nueva cuota
        if new_payment > available_capacity:
            return "RECHAZADO", f"La cuota mensual (${new_payment}) excede la capacidad disponible (${available_capacity})"
        
        # Verificar si es un monto alto (más de 5 salarios)
        high_amount_threshold = base_salary * self.HIGH_AMOUNT_THRESHOLD_MULTIPLIER
        if loan_amount > high_amount_threshold:
            return "REVISION_MANUAL", f"Monto alto requiere revisión manual. Préstamo: ${loan_amount}, Límite: ${high_amount_threshold}"
        
        return "APROBADO", f"Capacidad suficiente. Cuota: ${new_payment}, Disponible: ${available_capacity}"

# ========================================
# ADAPTADORES DE INFRAESTRUCTURA
# ========================================

class SQSStatusUpdateService(StatusUpdateService):
    """Adaptador para enviar actualización de estado vía SQS de vuelta al LoadRequestService"""
    
    def __init__(self):
        self.sqs = boto3.client('sqs')
        self.status_update_queue_url = os.getenv('STATUS_UPDATE_QUEUE_URL')
        
    def update_loan_status(self, loan_id: int, status: str, reason: str) -> bool:
        """Envía actualización de estado vía SQS"""
        try:
            if not self.status_update_queue_url:
                logger.warning("STATUS_UPDATE_QUEUE_URL no configurada, solo logging el estado")
                logger.info(f"Estado para solicitud {loan_id}: {status} - {reason}")
                return True
                
            message = {
                'event_type': 'LOAN_STATUS_UPDATE',
                'loan_request_id': loan_id,
                'new_status': status,
                'reason': reason,
                'timestamp': datetime.now().isoformat(),
                'source': 'DebtCapacityService'
            }
            
            response = self.sqs.send_message(
                QueueUrl=self.status_update_queue_url,
                MessageBody=json.dumps(message)
            )
            
            logger.info(f"Actualización de estado enviada para solicitud {loan_id}: {status}")
            return True
            
        except Exception as e:
            logger.error(f"Error enviando actualización de estado para solicitud {loan_id}: {str(e)}")
            return False

class SQSStatusUpdateService(StatusUpdateService):
    """Adaptador para actualizar estado vía SQS usando cola separada para responses"""
    
    def __init__(self):
        self.sqs = boto3.client('sqs')
        # Usar cola separada para respuestas, fallback a cola de requests si no existe
        self.response_queue_url = os.getenv('DEBT_CAPACITY_RESPONSE_QUEUE_URL', os.getenv('DEBT_CAPACITY_REQUEST_QUEUE_URL'))
        logger.warning(f"🔧 Response queue configurada: {self.response_queue_url}")
        
    def update_loan_status(self, loan_id: int, status: str, reason: str) -> bool:
        """Envía respuesta por cola separada - evita competencia con Lambda"""
        try:
            if not self.response_queue_url:
                logger.error("No hay cola de response configurada para enviar respuesta")
                return False
            
            # Crear mensaje de respuesta - SIMPLE
            response_message = {
                'event_type': 'DEBT_CAPACITY_RESPONSE',
                'loan_request_id': loan_id,
                'status': status,
                'reason': reason,
                'timestamp': datetime.now().isoformat(),
                'source': 'DebtCapacityService'
            }
            
            logger.info(f"Enviando respuesta SQS a cola separada para solicitud {loan_id}: {status}")
            
            # Enviar a cola SEPARADA para respuestas - evita competencia
            response = self.sqs.send_message(
                QueueUrl=self.response_queue_url,
                MessageBody=json.dumps(response_message),
                MessageAttributes={
                    'MessageType': {
                        'DataType': 'String',
                        'StringValue': 'RESPONSE'
                    },
                    'LoanRequestId': {
                        'DataType': 'String', 
                        'StringValue': str(loan_id)
                    }
                }
            )
            
            logger.info(f"Respuesta enviada a cola separada. MessageId: {response.get('MessageId')}")
            return True
            
        except Exception as e:
            logger.error(f"Error enviando respuesta bidireccional: {str(e)}")
            return False

class SQSNotificationService(NotificationService):
    """Adaptador para envío de notificaciones vía SQS"""
    
    def __init__(self):
        self.sqs = boto3.client('sqs')
        self.queue_url = os.getenv('NOTIFICATION_QUEUE_URL')
    
    def send_email_notification(self, loan_request_id: int, client_email: str, 
                               decision: str, payment_plan: List[PaymentPlan]) -> bool:
        """Envía notificación por email a través de SQS"""
        try:
            # Preparar plan de pago para el email
            plan_data = []
            for payment in payment_plan:
                plan_data.append({
                    'cuota': payment.period,
                    'abono_capital': float(payment.principal_payment),
                    'pago_interes': float(payment.interest_payment),
                    'cuota_total': float(payment.total_payment),
                    'saldo_restante': float(payment.remaining_balance)
                })
            
            message = {
                'type': 'LOAN_DECISION',
                'loan_request_id': loan_request_id,
                'client_email': client_email,
                'decision': decision,
                'payment_plan': plan_data,
                'timestamp': datetime.now().isoformat()
            }
            
            response = self.sqs.send_message(
                QueueUrl=self.queue_url,
                MessageBody=json.dumps(message)
            )
            
            logger.info(f"Notificación enviada para solicitud {loan_request_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error enviando notificación para solicitud {loan_request_id}: {str(e)}")
            return False

# ========================================
# CONTROLADOR DE LA LAMBDA
# ========================================

class DebtCapacityController:
    """Controlador principal de la Lambda"""
    
    def __init__(self):
        self.notification_service = SQSNotificationService()
        # Usar SQS para actualizaciones de estado (Request-Response Pattern)
        self.status_update_service = SQSStatusUpdateService()
        self.calculator = DebtCapacityCalculator(self.notification_service, self.status_update_service)
    
    def handle_request(self, event: Dict[str, Any]) -> Dict[str, Any]:
        """Maneja la solicitud de cálculo de capacidad"""
        try:
            logger.warning("Iniciando procesamiento de solicitud de capacidad de endeudamiento")
            logger.warning(f"Event recibido: {json.dumps(event, default=str)}")
            
            # Log configuración de conectividad
            loan_service_url = os.getenv('LOAN_REQUEST_SERVICE_URL', 'http://localhost:8081')
            logger.info(f"Configuración LoadRequestService URL: {loan_service_url}")
            
            # Extraer datos completos del evento SQS
            message_data = self._extract_complete_data_from_event(event)
            
            # Si el mensaje debe ser ignorado (respuesta propia), retornar éxito silenciosamente
            if message_data is None:
                logger.warning("✅ Mensaje ignorado exitosamente")
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': 'Mensaje ignorado - respuesta propia',
                        'status': 'ignored'
                    })
                }
            
            # Crear request para el cálculo con todos los datos
            capacity_request = DebtCapacityRequest(
                loan_request_id=message_data['current_loan']['loan_request_id'],
                client_document=message_data['client']['document'],
                amount=Decimal(str(message_data['current_loan']['amount'])),
                term_in_months=message_data['current_loan']['term_in_months'],
                loan_type=message_data['current_loan']['loan_type'],
                interest_rate=Decimal(str(message_data['current_loan']['interest_rate'])),
                base_salary=Decimal(str(message_data['client']['base_salary'])),
                client_email=message_data['client']['email'],
                client_name=f"{message_data['client']['first_name']} {message_data['client']['last_name']}",
                approved_loans=message_data['approved_loans']
            )
            
            # Realizar el cálculo
            result = self.calculator.calculate_debt_capacity(capacity_request)
            
            # Actualizar el estado 
            new_status = self._map_decision_to_status(result.decision)
            self.status_update_service.update_loan_status(
                capacity_request.loan_request_id, new_status, result.reason
            )
            
            # Enviar notificación por email
            self.notification_service.send_email_notification(
                capacity_request.loan_request_id, capacity_request.client_email, 
                result.decision, result.payment_plan
            )
            
            # Preparar respuesta
            response = {
                'statusCode': 200,
                'body': {
                    'loan_request_id': result.loan_request_id,
                    'decision': result.decision,
                    'reason': result.reason,
                    'max_debt_capacity': float(result.max_debt_capacity),
                    'current_monthly_debt': float(result.current_monthly_debt),
                    'available_capacity': float(result.available_capacity),
                    'new_loan_payment': float(result.new_loan_payment),
                    'payment_plan_summary': {
                        'total_payments': len(result.payment_plan),
                        'total_interest': float(sum(p.interest_payment for p in result.payment_plan)),
                        'total_amount': float(sum(p.total_payment for p in result.payment_plan))
                    }
                }
            }
            
            logger.info(f"Procesamiento completado exitosamente para solicitud {capacity_request.loan_request_id}")
            return response
            
        except Exception as e:
            logger.error(f"Error procesando solicitud: {str(e)}")
            logger.error(traceback.format_exc())
            
            return {
                'statusCode': 500,
                'body': {
                    'error': 'Error interno del servidor',
                    'message': str(e) if os.getenv('DEBUG') == 'true' else 'Ha ocurrido un error inesperado'
                }
            }
    
    def _extract_complete_data_from_event(self, event: Dict[str, Any]) -> Dict[str, Any]:
        """Extrae todos los datos del evento SQS"""
        try:
            logger.warning(f"🔍 DEBUG - Estructura del evento recibido: {json.dumps(event, default=str, indent=2)}")
            
            # Para eventos de SQS (caso principal)
            if 'Records' in event:
                logger.warning(f"📨 Procesando evento SQS con {len(event['Records'])} records")
                for record in event['Records']:
                    if 'body' in record:
                        logger.warning(f"📋 Body del record: {record['body']}")
                        body = json.loads(record['body'])
                        logger.warning(f"📋 Body parseado: {json.dumps(body, default=str)}")
                        
                        # IGNORAR respuestas - son para LoadRequestService, no para este Lambda
                        if 'event_type' in body and body['event_type'] == 'DEBT_CAPACITY_RESPONSE':
                            logger.warning("🔄 IGNORANDO respuesta propia - este mensaje es para LoadRequestService")
                            # Retornar None para indicar que debe ser ignorado silenciosamente
                            return None
                        
                        if 'event_type' in body and body['event_type'] == 'AUTOMATIC_DEBT_CAPACITY_VALIDATION':
                            logger.warning("✅ Encontrado evento de validación automática en SQS")
                            return body
                            
            # Para invocación directa (testing)
            if 'event_type' in event and event['event_type'] == 'AUTOMATIC_DEBT_CAPACITY_VALIDATION':
                logger.info("✅ Encontrado evento de validación automática directo")
                return event
                
            # Fallback para casos de testing simples
            if 'loan_request_id' in event:
                logger.info(f"⚠️ Usando fallback para loan_request_id: {event['loan_request_id']}")
                # Crear estructura mínima para testing
                return {
                    'current_loan': {
                        'loan_request_id': event['loan_request_id'],
                        'amount': 5000000,
                        'term_in_months': 36,
                        'loan_type': 'PERSONAL',
                        'interest_rate': 15.5
                    },
                    'client': {
                        'document': '12345678',
                        'first_name': 'Test',
                        'last_name': 'Client',
                        'email': 'test@crediya.com',
                        'base_salary': 3000000
                    },
                    'approved_loans': []
                }
            
            # Fallback genérico para cualquier evento
            logger.warning("⚠️ Evento no reconocido, usando datos de prueba")
            return {
                'current_loan': {
                    'loan_request_id': 999,
                    'amount': 5000000,
                    'term_in_months': 36,
                    'loan_type': 'PERSONAL',
                    'interest_rate': 15.5
                },
                'client': {
                    'document': '12345678',
                    'first_name': 'Test',
                    'last_name': 'Client',
                    'email': 'test@crediya.com',
                    'base_salary': 3000000
                },
                'approved_loans': []
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"❌ Error parsing JSON del evento: {str(e)}")
            raise ValueError(f"Error parsing JSON del evento: {str(e)}")
        except Exception as e:
            logger.error(f"❌ Error extrayendo datos del evento: {str(e)}")
            raise ValueError(f"Error extrayendo datos del evento: {str(e)}")
    
    def _extract_loan_request_id(self, event: Dict[str, Any]) -> int:
        """Extrae el ID de solicitud del evento"""
        # Para invocación directa
        if 'loan_request_id' in event:
            return int(event['loan_request_id'])
        
        # Para eventos de SQS
        if 'Records' in event:
            for record in event['Records']:
                if 'body' in record:
                    body = json.loads(record['body'])
                    if 'loan_request_id' in body:
                        return int(body['loan_request_id'])
        
        # Para eventos de API Gateway
        if 'pathParameters' in event and event['pathParameters']:
            if 'id' in event['pathParameters']:
                return int(event['pathParameters']['id'])
        
        # Para body de POST
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
            if 'loan_request_id' in body:
                return int(body['loan_request_id'])
        
        raise ValueError("No se pudo encontrar loan_request_id en el evento")
    
    def _map_decision_to_status(self, decision: str) -> str:
        """Mapea la decisión a un estado de base de datos"""
        mapping = {
            'APROBADO': 'APPROVED',
            'RECHAZADO': 'REJECTED',
            'REVISION_MANUAL': 'MANUAL_REVIEW'
        }
        return mapping.get(decision, 'PENDING_REVIEW')
    

# ========================================
# PUNTO DE ENTRADA DE LA LAMBDA
# ========================================

# Instancia global del controlador
controller = DebtCapacityController()

def lambda_handler(event, context):
    """Función principal de la Lambda"""
    logger.warning("=== INICIO PROCESAMIENTO DEBT CAPACITY ===")
    logger.warning(f"🔍 LAMBDA EVENT COMPLETO: {json.dumps(event, default=str, indent=2)}")
    return controller.handle_request(event)

# Para testing local
if __name__ == "__main__":
    # Ejemplo de evento de prueba
    test_event = {
        "loan_request_id": 1
    }
    
    result = lambda_handler(test_event, None)
    print(json.dumps(result, indent=2, default=str))
