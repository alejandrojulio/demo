import json
import boto3
import logging
import time
from datetime import datetime
from typing import Dict, Any
import os

logger = logging.getLogger()
logger.setLevel(logging.INFO)

ses_client = boto3.client('ses')

SENDER_EMAIL = os.environ.get('SENDER_EMAIL', 'noreply@crediya.com')
AWS_REGION = os.environ.get('AWS_REGION', 'us-east-2')

def lambda_handler(event, context):
    """Procesa mensajes de SQS y envía notificaciones por email usando SES"""
    try:
        for record in event.get('Records', []):
            process_sqs_message(record)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Notificaciones procesadas exitosamente',
                'processed_records': len(event.get('Records', []))
            })
        }
    
    except Exception as e:
        logger.error(f"Error procesando notificaciones: {str(e)}")
        raise e

def process_sqs_message(record: Dict[str, Any]):
    """Procesa un mensaje individual de SQS y enruta según el tipo de evento"""
    try:
        message_body = json.loads(record['body'])
        actual_message = extract_message_data(message_body)
        
        if not validate_message_structure(actual_message):
            logger.error(f"Mensaje con estructura inválida: {actual_message}")
            return
        
        event_type = actual_message.get('type', actual_message.get('eventType'))
        
        if event_type == 'LOAN_APPROVED':
            send_loan_approved_notification(actual_message)
        elif event_type == 'LOAN_REJECTED':
            send_loan_rejected_notification(actual_message)
        elif event_type == 'LOAN_DECISION':
            send_debt_capacity_decision_notification(actual_message)
        else:
            logger.warning(f"Tipo de evento no reconocido: {event_type}")
    
    except json.JSONDecodeError as e:
        logger.error(f"Error parsing JSON del mensaje SQS: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Error procesando mensaje SQS: {str(e)}")
        raise e

def extract_message_data(message_body: Dict[str, Any]) -> Dict[str, Any]:
    """Extrae datos del mensaje manejando estructuras directas y anidadas"""
    try:
        if 'type' in message_body or 'eventType' in message_body:
            return message_body
        
        if 'body' in message_body and isinstance(message_body['body'], dict):
            return message_body['body']
        
        return message_body
        
    except Exception as e:
        logger.error(f"Error extrayendo datos del mensaje: {str(e)}")
        return message_body

def validate_message_structure(message: Dict[str, Any]) -> bool:
    """Valida que el mensaje tenga los campos mínimos requeridos según su tipo"""
    try:
        event_type = message.get('type', message.get('eventType'))
        if not event_type:
            logger.error("Mensaje sin tipo de evento (type o eventType)")
            return False
        
        if event_type == 'LOAN_DECISION':
            required_fields = ['client_email', 'decision', 'loan_request_id']
            for field in required_fields:
                if field not in message:
                    logger.error(f"Campo requerido faltante para LOAN_DECISION: {field}")
                    return False
        elif event_type in ['LOAN_APPROVED', 'LOAN_REJECTED']:
            required_fields = ['clientEmail', 'solicitudId']
            for field in required_fields:
                if field not in message:
                    logger.error(f"Campo requerido faltante para {event_type}: {field}")
                    return False
        
        return True
        
    except Exception as e:
        logger.error(f"Error validando estructura del mensaje: {str(e)}")
        return False

def send_loan_approved_notification(message: Dict[str, Any]):
    """Envía notificación de préstamo aprobado manualmente por asesor"""
    try:
        client_email = message.get('clientEmail')
        client_name = message.get('clientName')
        solicitud_id = message.get('solicitudId')
        monto_aprobado = message.get('montoAprobado')
        tasa_interes = message.get('tasaInteres')
        plazo_aprobado = message.get('plazoAprobado')
        pago_mensual = message.get('pagoMensual')
        
        logger.info(f"Procesando notificación de aprobación para solicitud {solicitud_id}, cliente: {client_email}, nombre: {client_name}")
        
        subject = f"¡Crédito Aprobado! - Solicitud #{solicitud_id}"
        
        text_body = f"""¡FELICITACIONES! Tu crédito ha sido APROBADO.

==================================================
                 CRÉDITO APROBADO
==================================================

Hola {client_name},

Nos complace informarte que tu solicitud de crédito 
ha sido APROBADA por nuestro equipo.

DETALLES DE TU CRÉDITO:
• Solicitud: #{solicitud_id}
• Monto aprobado: {monto_aprobado}
• Tasa de interés: {tasa_interes}
• Plazo: {plazo_aprobado} meses
• Cuota mensual: {pago_mensual}

==================================================
                PRÓXIMOS PASOS
==================================================

• Un asesor te contactará en las próximas horas
• Prepara tus documentos de identificación
• Ten a la mano tu información laboral actualizada

==================================================
                   CONTACTO
==================================================

Si tienes preguntas:
Teléfono: (01) 234-5678
Email: info@crediya.com
Horarios: Lunes a Viernes 8:00 AM - 6:00 PM

¡Gracias por confiar en CrediYa!

Atentamente,
Equipo CrediYa

--
Este mensaje fue generado por nuestro sistema 
de gestión de créditos."""
        
        send_email_notification(client_email, subject, text_body)
    
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_loan_rejected_notification(message: Dict[str, Any]):
    """Envía notificación de préstamo rechazado manualmente por asesor"""
    try:
        client_email = message.get('clientEmail')
        client_name = message.get('clientName')
        solicitud_id = message.get('solicitudId')
        reason = message.get('reason', 'No especificado')
        
        subject = f"Resultado de tu solicitud #{solicitud_id}"
        
        text_body = f"""RESULTADO DE TU SOLICITUD DE CRÉDITO

==================================================
            SOLICITUD NO APROBADA
==================================================

Hola {client_name},

Lamentamos informarte que tu solicitud de crédito 
#{solicitud_id} no ha sido aprobada en esta ocasión.

MOTIVO:
{reason}

==================================================
               ¿QUÉ PUEDES HACER?
==================================================

• Mejorar tu perfil crediticio
• Aumentar tus ingresos declarados  
• Reducir tus deudas actuales
• Aplicar nuevamente en 30 días

==================================================
                   CONTACTO
==================================================

Para asesoría personalizada:
Teléfono: (01) 234-5678
Email: asesoria@crediya.com
Horarios: Lunes a Viernes 8:00 AM - 6:00 PM

No te desanimes, estamos aquí para ayudarte a 
mejorar tu perfil crediticio.

Atentamente,
Equipo CrediYa

--
Este mensaje fue generado por nuestro sistema 
de gestión de créditos."""
        
        send_email_notification(client_email, subject, text_body)
    
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

def send_debt_capacity_decision_notification(message: Dict[str, Any]):
    """Enruta notificación según decisión de validación automática de capacidad"""
    try:
        client_email = message.get('client_email')
        decision = message.get('decision')
        loan_request_id = message.get('loan_request_id')
        payment_plan = message.get('payment_plan', [])
        
        if not client_email:
            logger.error("Email del cliente no proporcionado")
            return
        
        if decision == 'APROBADO':
            send_approved_with_payment_plan_text(client_email, loan_request_id, payment_plan)
        elif decision == 'RECHAZADO':
            send_rejected_notification_text(client_email, loan_request_id, message.get('reason', 'No especificado'))
        elif decision == 'REVISION_MANUAL':
            send_manual_review_notification_text(client_email, loan_request_id)
        else:
            logger.warning(f"Decisión no reconocida: {decision}")
            
    except Exception as e:
        logger.error(f"Error enviando notificación de decisión de capacidad: {str(e)}")
        raise e

def send_approved_with_payment_plan_text(email: str, loan_request_id: int, payment_plan: list):
    """Envía notificación de préstamo aprobado automáticamente con plan de pago"""
    try:
        subject = f"¡Préstamo Aprobado! - Solicitud #{loan_request_id}"
        
        total_interest = sum(float(payment.get('pago_interes', 0)) for payment in payment_plan)
        total_amount = sum(float(payment.get('cuota_total', 0)) for payment in payment_plan)
        monthly_payment = float(payment_plan[0].get('cuota_total', 0)) if payment_plan else 0
        
        payment_table_text = generate_payment_plan_text(payment_plan)
        
        text_body = f"""¡FELICITACIONES! Tu préstamo ha sido APROBADO automáticamente.

==================================================
                 CRÉDITO APROBADO
==================================================

Solicitud: #{loan_request_id}
Cuota mensual: ${monthly_payment:,.2f}
Total a pagar: ${total_amount:,.2f}
Total intereses: ${total_interest:,.2f}
Número de cuotas: {len(payment_plan)}

==================================================
               PLAN DE PAGO DETALLADO
==================================================

{payment_table_text}

==================================================
                PRÓXIMOS PASOS
==================================================

• Un asesor de CrediYa te contactará en las próximas 24 horas
• Mantén tus documentos de identificación actualizados
• Prepara la documentación adicional que pueda solicitarse

¡Gracias por confiar en CrediYa!

--
Este mensaje fue generado automáticamente por el 
sistema de validación de CrediYa.

Para consultas: (01) 234-5678
Email: info@crediya.com"""
        
        send_email_notification(email, subject, text_body)
        
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_rejected_notification_text(email: str, loan_request_id: int, reason: str):
    """Envía notificación de préstamo rechazado automáticamente"""
    try:
        subject = f"Resultado de tu solicitud #{loan_request_id}"
        
        text_body = f"""RESULTADO DE TU SOLICITUD DE CRÉDITO

==================================================
            SOLICITUD NO APROBADA
==================================================

Solicitud: #{loan_request_id}
Estado: NO APROBADA

Motivo: {reason}

==================================================
               ¿QUÉ PUEDES HACER?
==================================================

• Mejorar tu perfil crediticio
• Aumentar tus ingresos declarados  
• Reducir tus deudas actuales
• Aplicar nuevamente en 30 días

==================================================
                   CONTACTO
==================================================

Para asesoría personalizada:
Teléfono: (01) 234-5678
Email: asesoria@crediya.com
Horarios: Lunes a Viernes 8:00 AM - 6:00 PM

No te desanimes, estamos aquí para ayudarte a 
mejorar tu perfil crediticio.

Atentamente,
Equipo CrediYa"""
        
        send_email_notification(email, subject, text_body)
        
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

def send_manual_review_notification_text(email: str, loan_request_id: int):
    """Envía notificación de solicitud en revisión manual"""
    try:
        subject = f"Tu solicitud #{loan_request_id} está en revisión"
        
        text_body = f"""SOLICITUD EN REVISIÓN

==================================================
               REVISIÓN EN PROCESO
==================================================

Solicitud: #{loan_request_id}
Estado: EN REVISIÓN MANUAL

Tu solicitud está siendo evaluada cuidadosamente 
por nuestro equipo de análisis crediticio.

==================================================
                 CRONOGRAMA
==================================================

✓ Validación inicial completada
→ Análisis detallado en progreso  
⏳ Decisión final pendiente

Tiempo estimado: 1-2 días hábiles

==================================================
              ¿QUÉ SIGUE?
==================================================

• Te notificaremos por email tan pronto tengamos 
  una decisión
• Mantén tu teléfono disponible por si necesitamos
  información adicional
• No es necesario que nos contactes, nosotros 
  te escribiremos

==================================================
                   CONTACTO
==================================================

Si tienes preguntas urgentes:
Teléfono: (01) 234-5678
Email: revision@crediya.com

Gracias por tu paciencia.

Atentamente,
Equipo de Análisis Crediticio - CrediYa"""
        
        send_email_notification(email, subject, text_body)
        
    except Exception as e:
        logger.error(f"Error enviando notificación de revisión manual: {str(e)}")
        raise e


def generate_payment_plan_text(payment_plan: list) -> str:
    """Genera tabla de texto plano del plan de pagos con formato estructurado"""
    if not payment_plan:
        return "No hay plan de pago disponible."
    
    text = "Cuota | Abono Capital | Pago Interés | Cuota Total  | Saldo Restante\n"
    text += "------|---------------|--------------|--------------|---------------\n"
    
    for payment in payment_plan:
        cuota = payment.get('cuota', 0)
        abono_capital = float(payment.get('abono_capital', 0))
        pago_interes = float(payment.get('pago_interes', 0))
        cuota_total = float(payment.get('cuota_total', 0))
        saldo_restante = float(payment.get('saldo_restante', 0))
        
        text += f"{cuota:5d} | ${abono_capital:11,.2f} | ${pago_interes:10,.2f} | ${cuota_total:10,.2f} | ${saldo_restante:13,.2f}\n"
    
    total_capital = sum(float(p.get('abono_capital', 0)) for p in payment_plan)
    total_interes = sum(float(p.get('pago_interes', 0)) for p in payment_plan)
    total_cuotas = sum(float(p.get('cuota_total', 0)) for p in payment_plan)
    
    text += "------|---------------|--------------|--------------|---------------\n"
    text += f"TOTAL | ${total_capital:11,.2f} | ${total_interes:10,.2f} | ${total_cuotas:10,.2f} |\n"
    
    return text

def generate_payment_plan_html(payment_plan: list) -> str:
    """Genera tabla HTML del plan de pagos con estilos CSS"""
    if not payment_plan:
        return "<p>No hay plan de pago disponible.</p>"
    
    html = """<table class="table">
        <thead>
            <tr>
                <th>Cuota</th>
                <th>Abono Capital</th>
                <th>Pago Interés</th>
                <th>Cuota Total</th>
                <th>Saldo Restante</th>
            </tr>
        </thead>
        <tbody>"""
    
    for payment in payment_plan:
        cuota = payment.get('cuota', 0)
        abono_capital = float(payment.get('abono_capital', 0))
        pago_interes = float(payment.get('pago_interes', 0))
        cuota_total = float(payment.get('cuota_total', 0))
        saldo_restante = float(payment.get('saldo_restante', 0))
        
        html += f"""
            <tr>
                <td>{cuota}</td>
                <td>${abono_capital:,.2f}</td>
                <td>${pago_interes:,.2f}</td>
                <td>${cuota_total:,.2f}</td>
                <td>${saldo_restante:,.2f}</td>
            </tr>"""
    
    html += """
        </tbody>
    </table>"""
    
    return html

def send_email_notification(email: str, subject: str, text_body: str, html_body: str = None):
    """Envía notificación por email usando SES directamente"""
    try:
        if not email or not email.strip():
            logger.error("Email del destinatario está vacío o no válido")
            return
            
        # Construir el mensaje SES
        message = {
            'Subject': {
                'Data': f"CrediYa: {subject}",
                'Charset': 'UTF-8'
            },
            'Body': {}
        }
        
        # Añadir texto plano
        if text_body:
            message['Body']['Text'] = {
                'Data': text_body,
                'Charset': 'UTF-8'
            }
        
        # Añadir HTML si está disponible
        if html_body:
            message['Body']['Html'] = {
                'Data': html_body,
                'Charset': 'UTF-8'
            }
        
        # Enviar el email usando SES
        response = ses_client.send_email(
            Source=SENDER_EMAIL,
            Destination={'ToAddresses': [email.strip()]},
            Message=message
        )
        
        logger.info(f"Email enviado exitosamente a {email}. MessageId: {response['MessageId']}")
        
    except ses_client.exceptions.MessageRejected as e:
        logger.error(f"Email rechazado por SES para {email}: {str(e)}")
        raise e
    except ses_client.exceptions.MailFromDomainNotVerifiedException as e:
        logger.error(f"Dominio de origen no verificado en SES: {str(e)}")
        raise e
    except ses_client.exceptions.ConfigurationSetDoesNotExistException as e:
        logger.error(f"Configuration set no existe en SES: {str(e)}")
        raise e
    except Exception as e:
        logger.error(f"Error enviando email a {email}: {str(e)}")
        raise e
