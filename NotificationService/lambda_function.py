import json
import boto3
import logging
import time
from datetime import datetime
from typing import Dict, Any
import os

# Configurar logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Clientes AWS
sns_client = boto3.client('sns')

# Variables de entorno
SNS_TOPIC_ARN = os.environ.get('SNS_TOPIC_ARN')
SENDER_EMAIL = os.environ.get('SENDER_EMAIL', 'noreply@crediya.com')

def lambda_handler(event, context):
    """
    Función principal de la Lambda que procesa mensajes de SQS
    y envía notificaciones por email usando SNS
    """
    logger.info(f"Procesando evento: {json.dumps(event)}")
    
    try:
        # Procesar cada mensaje en el batch de SQS
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
    """
    Procesa un mensaje individual de SQS
    """
    try:
        # Extraer el cuerpo del mensaje
        message_body = json.loads(record['body'])
        logger.info(f"Procesando mensaje: {record.get('messageId', 'N/A')}")
        logger.info(f"Contenido del mensaje: {json.dumps(message_body, indent=2)}")
        
        # Extraer los datos del mensaje (manejar estructuras anidadas)
        actual_message = extract_message_data(message_body)
        
        # Validar que el mensaje tenga los campos básicos requeridos
        if not validate_message_structure(actual_message):
            logger.error(f"Mensaje con estructura inválida: {actual_message}")
            return
        
        # Determinar el tipo de notificación
        event_type = actual_message.get('type', actual_message.get('eventType'))
        logger.info(f"🎯 Tipo de evento detectado: {event_type}")
        
        if event_type == 'LOAN_APPROVED':
            logger.info(f"📧 Procesando aprobación manual para: {actual_message.get('clientEmail', 'N/A')}")
            send_loan_approved_notification(actual_message)
        elif event_type == 'LOAN_REJECTED':
            logger.info(f"📧 Procesando rechazo manual para: {actual_message.get('clientEmail', 'N/A')}")
            send_loan_rejected_notification(actual_message)
        elif event_type == 'LOAN_DECISION':
            logger.info(f"📧 Procesando decisión automática para: {actual_message.get('client_email', 'N/A')}")
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
    """
    Extrae los datos del mensaje manejando diferentes estructuras
    """
    try:
        # Caso 1: Estructura directa (DebtCapacityService)
        # {"type": "LOAN_DECISION", "client_email": "...", ...}
        if 'type' in message_body or 'eventType' in message_body:
            logger.info("📋 Estructura directa detectada")
            return message_body
        
        # Caso 2: Estructura anidada (LoadRequestService) 
        # {"body": {"eventType": "LOAN_APPROVED", "clientEmail": "...", ...}}
        if 'body' in message_body and isinstance(message_body['body'], dict):
            logger.info("📋 Estructura anidada detectada")
            return message_body['body']
        
        # Caso 3: Fallback - retornar tal como está
        logger.warning("⚠️ Estructura no reconocida, usando mensaje completo")
        return message_body
        
    except Exception as e:
        logger.error(f"Error extrayendo datos del mensaje: {str(e)}")
        return message_body

def validate_message_structure(message: Dict[str, Any]) -> bool:
    """
    Valida que el mensaje tenga la estructura mínima requerida
    """
    try:
        # Verificar que tenga tipo de evento - corregir orden de prioridad
        event_type = message.get('type', message.get('eventType'))
        if not event_type:
            logger.error("Mensaje sin tipo de evento (type o eventType)")
            return False
        
        # Verificar campos específicos según el tipo de evento
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
    """
    Envía notificación de préstamo aprobado manualmente por asesor (texto plano)
    """
    try:
        client_email = message.get('clientEmail')
        client_name = message.get('clientName')
        solicitud_id = message.get('solicitudId')
        monto_aprobado = message.get('montoAprobado')
        tasa_interes = message.get('tasaInteres')
        plazo_aprobado = message.get('plazoAprobado')
        pago_mensual = message.get('pagoMensual')
        
        # Crear el mensaje de email
        subject = f"¡Crédito Aprobado! - Solicitud #{solicitud_id}"
        
        # Mensaje de texto plano profesional
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
        
        logger.info(f"Notificación de aprobación enviada a {client_email} para solicitud {solicitud_id}")
    
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_loan_rejected_notification(message: Dict[str, Any]):
    """
    Envía notificación de préstamo rechazado manualmente por asesor (texto plano)
    """
    try:
        client_email = message.get('clientEmail')
        client_name = message.get('clientName')
        solicitud_id = message.get('solicitudId')
        reason = message.get('reason', 'No especificado')
        
        # Crear el mensaje de email
        subject = f"Resultado de tu solicitud #{solicitud_id}"
        
        # Mensaje de texto plano profesional
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
        
        logger.info(f"Notificación de rechazo enviada a {client_email} para solicitud {solicitud_id}")
    
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

<<<<<<< HEAD
def send_debt_capacity_decision_notification(message: Dict[str, Any]):
    """
    Envía notificación con el resultado de la validación automática de capacidad de endeudamiento
    """
    try:
        client_email = message.get('client_email')
        decision = message.get('decision')
        loan_request_id = message.get('loan_request_id')
        payment_plan = message.get('payment_plan', [])
        
        if not client_email:
            logger.error("Email del cliente no proporcionado")
            return
        
        # Crear el mensaje según la decisión
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
    """
    Envía notificación de préstamo aprobado con plan de pago en texto plano
    """
    try:
        subject = f"¡Préstamo Aprobado! - Solicitud #{loan_request_id}"
        
        # Calcular totales del plan de pago
        total_interest = sum(float(payment.get('pago_interes', 0)) for payment in payment_plan)
        total_amount = sum(float(payment.get('cuota_total', 0)) for payment in payment_plan)
        monthly_payment = float(payment_plan[0].get('cuota_total', 0)) if payment_plan else 0
        
        # Generar tabla de texto plano del plan de pago
        payment_table_text = generate_payment_plan_text(payment_plan)
        
        # Mensaje de texto completo
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
        logger.info(f"Notificación de aprobación enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_approved_with_payment_plan(email: str, loan_request_id: int, payment_plan: list):
    """
    Envía notificación de préstamo aprobado con plan de pago detallado
    """
    try:
        subject = f"¡Préstamo Aprobado! - Solicitud #{loan_request_id}"
        
        # Calcular totales del plan de pago
        total_interest = sum(float(payment.get('pago_interes', 0)) for payment in payment_plan)
        total_amount = sum(float(payment.get('cuota_total', 0)) for payment in payment_plan)
        monthly_payment = float(payment_plan[0].get('cuota_total', 0)) if payment_plan else 0
        
        # Mensaje de texto plano
        text_body = f"""¡Felicitaciones! Tu préstamo ha sido APROBADO automáticamente.

Solicitud: #{loan_request_id}
Cuota mensual: ${monthly_payment:,.2f}
Total a pagar: ${total_amount:,.2f}
Total intereses: ${total_interest:,.2f}
Número de cuotas: {len(payment_plan)}

Plan de pago adjunto en el email.

Un asesor te contactará para formalizar el crédito.

Atentamente,
CrediYa - Sistema Automático"""

        # Generar tabla HTML del plan de pago
        payment_table_html = generate_payment_plan_html(payment_plan)
        
        # Mensaje HTML completo
        html_body = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .header {{ background-color: #2e7d32; color: white; padding: 20px; text-align: center; }}
        .content {{ padding: 20px; }}
        .summary {{ background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }}
        .table {{ width: 100%; border-collapse: collapse; margin: 20px 0; }}
        .table th, .table td {{ border: 1px solid #ddd; padding: 8px; text-align: right; }}
        .table th {{ background-color: #2e7d32; color: white; }}
        .table tr:nth-child(even) {{ background-color: #f9f9f9; }}
        .footer {{ margin-top: 30px; padding: 20px; background-color: #f0f0f0; text-align: center; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>¡Préstamo Aprobado!</h1>
        <p>Tu solicitud #{loan_request_id} ha sido procesada automáticamente</p>
    </div>
    
    <div class="content">
        <div class="summary">
            <h3>Resumen del Préstamo</h3>
            <p><strong>Cuota mensual:</strong> ${monthly_payment:,.2f}</p>
            <p><strong>Total a pagar:</strong> ${total_amount:,.2f}</p>
            <p><strong>Total intereses:</strong> ${total_interest:,.2f}</p>
            <p><strong>Número de cuotas:</strong> {len(payment_plan)}</p>
        </div>
        
        <h3>Plan de Pago Detallado</h3>
        {payment_table_html}
        
        <div class="footer">
            <p><strong>Próximos pasos:</strong></p>
            <p>Un asesor de CrediYa te contactará en las próximas 24 horas para formalizar tu crédito.</p>
            <p>Mantén tus documentos de identificación actualizados.</p>
            <br>
            <p><em>Este mensaje fue generado automáticamente por el sistema de validación de CrediYa.</em></p>
        </div>
    </div>
</body>
</html>"""
        
        send_email_notification(email, subject, text_body, html_body)
        logger.info(f"Notificación de aprobación con plan de pago enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_rejected_notification_text(email: str, loan_request_id: int, reason: str):
    """
    Envía notificación de préstamo rechazado en texto plano
    """
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
        logger.info(f"Notificación de rechazo enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

def send_manual_review_notification_text(email: str, loan_request_id: int):
    """
    Envía notificación de revisión manual en texto plano
    """
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
        logger.info(f"Notificación de revisión manual enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de revisión manual: {str(e)}")
        raise e

def send_rejected_notification(email: str, loan_request_id: int, reason: str):
    """
    Envía notificación de préstamo rechazado
    """
    try:
        subject = f"Resultado de tu solicitud #{loan_request_id}"
        
        text_body = f"""Tu solicitud de préstamo #{loan_request_id} no fue aprobada.

Motivo: {reason}

Puedes mejorar tu perfil crediticio y aplicar nuevamente en 30 días.
Para asesoría personalizada: (01) 234-5678

Atentamente,
CrediYa"""

        html_body = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .header {{ background-color: #d32f2f; color: white; padding: 20px; text-align: center; }}
        .content {{ padding: 20px; }}
        .info-box {{ background-color: #fff3e0; padding: 15px; border-left: 4px solid #ff9800; margin: 20px 0; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Resultado de tu Solicitud</h1>
        <p>Solicitud #{loan_request_id}</p>
    </div>
    
    <div class="content">
        <p>Estimado cliente,</p>
        <p>Después de evaluar tu solicitud de préstamo, lamentamos informarte que no fue aprobada en esta ocasión.</p>
        
        <div class="info-box">
            <h3>Motivo:</h3>
            <p>{reason}</p>
        </div>
        
        <h3>¿Qué puedes hacer?</h3>
        <ul>
            <li>Mejorar tu perfil crediticio</li>
            <li>Aumentar tus ingresos declarados</li>
            <li>Reducir tus deudas actuales</li>
            <li>Aplicar nuevamente en 30 días</li>
        </ul>
        
        <p>Para asesoría personalizada, contáctanos al <strong>(01) 234-5678</strong></p>
        
        <p>Atentamente,<br><strong>CrediYa</strong></p>
    </div>
</body>
</html>"""
        
        send_email_notification(email, subject, text_body, html_body)
        logger.info(f"Notificación de rechazo enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

def send_manual_review_notification(email: str, loan_request_id: int):
    """
    Envía notificación de que la solicitud requiere revisión manual
    """
    try:
        subject = f"Tu solicitud #{loan_request_id} está en revisión"
        
        text_body = f"""Tu solicitud de préstamo #{loan_request_id} está siendo revisada por nuestro equipo.

La solicitud requiere evaluación manual debido al monto solicitado.

Tiempo estimado de respuesta: 1-2 días hábiles.
Te notificaremos tan pronto tengamos una decisión.

Atentamente,
CrediYa"""

        html_body = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; }}
        .header {{ background-color: #ff9800; color: white; padding: 20px; text-align: center; }}
        .content {{ padding: 20px; }}
        .timeline {{ background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin: 20px 0; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Solicitud en Revisión</h1>
        <p>Solicitud #{loan_request_id}</p>
    </div>
    
    <div class="content">
        <p>Estimado cliente,</p>
        <p>Tu solicitud de préstamo está siendo revisada cuidadosamente por nuestro equipo de análisis crediticio.</p>
        
        <div class="timeline">
            <h3>Proceso de Revisión</h3>
            <p>✅ Validación inicial completada</p>
            <p>🔄 Análisis detallado en progreso</p>
            <p>⏳ Decisión final pendiente</p>
        </div>
        
        <p><strong>Tiempo estimado:</strong> 1-2 días hábiles</p>
        <p>Te notificaremos por email tan pronto tengamos una decisión.</p>
        
        <p>Gracias por tu paciencia.</p>
        <p><strong>CrediYa</strong></p>
    </div>
</body>
</html>"""
        
        send_email_notification(email, subject, text_body, html_body)
        logger.info(f"Notificación de revisión manual enviada a {email} para solicitud {loan_request_id}")
        
    except Exception as e:
        logger.error(f"Error enviando notificación de revisión manual: {str(e)}")
        raise e

def generate_payment_plan_text(payment_plan: list) -> str:
    """
    Genera la tabla de texto plano del plan de pago
    """
    if not payment_plan:
        return "No hay plan de pago disponible."
    
    # Crear tabla de texto con formato fijo
    text = "Cuota | Abono Capital | Pago Interés | Cuota Total  | Saldo Restante\n"
    text += "------|---------------|--------------|--------------|---------------\n"
    
    for payment in payment_plan:
        cuota = payment.get('cuota', 0)
        abono_capital = float(payment.get('abono_capital', 0))
        pago_interes = float(payment.get('pago_interes', 0))
        cuota_total = float(payment.get('cuota_total', 0))
        saldo_restante = float(payment.get('saldo_restante', 0))
        
        # Formatear cada fila con espaciado fijo
        text += f"{cuota:5d} | ${abono_capital:11,.2f} | ${pago_interes:10,.2f} | ${cuota_total:10,.2f} | ${saldo_restante:13,.2f}\n"
    
    # Agregar totales al final
    total_capital = sum(float(p.get('abono_capital', 0)) for p in payment_plan)
    total_interes = sum(float(p.get('pago_interes', 0)) for p in payment_plan)
    total_cuotas = sum(float(p.get('cuota_total', 0)) for p in payment_plan)
    
    text += "------|---------------|--------------|--------------|---------------\n"
    text += f"TOTAL | ${total_capital:11,.2f} | ${total_interes:10,.2f} | ${total_cuotas:10,.2f} |\n"
    
    return text

def generate_payment_plan_html(payment_plan: list) -> str:
    """
    Genera la tabla HTML del plan de pago
    """
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

=======
>>>>>>> origin/main
def send_email_notification(email: str, subject: str, text_body: str, html_body: str = None):
    """
    Envía una notificación por email usando SNS con soporte para texto plano y HTML
    """
    try:
        # Crear el mensaje base
        message = {
            'email': email,
            'subject': subject,
            'text_body': text_body,
            'sender': SENDER_EMAIL,
            'timestamp': datetime.now().isoformat(),
            'format': 'multipart' if html_body else 'text'
        }
        
        # Agregar HTML si está disponible
        if html_body:
            message['html_body'] = html_body
        
        # Determinar el formato del mensaje SNS
        if html_body:
            # Para emails HTML, enviar el HTML directamente como mensaje principal
            # La mayoría de clientes de email interpretarán automáticamente el HTML
            sns_message = html_body
            message_structure = None
        else:
            # Mensaje simple de texto plano
            sns_message = text_body
            message_structure = None
        
        # Publicar en el tópico SNS con retry logic
        publish_params = {
            'TopicArn': SNS_TOPIC_ARN,
            'Subject': f"CrediYa: {subject}",
            'MessageAttributes': {
                'email_type': {
                    'DataType': 'String',
                    'StringValue': 'loan_decision'
                },
                'recipient': {
                    'DataType': 'String',
                    'StringValue': email
                },
                'format': {
                    'DataType': 'String',
                    'StringValue': message['format']
                },
                'content_type': {
                    'DataType': 'String',
                    'StringValue': 'text/html' if html_body else 'text/plain'
                }
            }
        }
        
        # El mensaje siempre se envía directamente (sin JSON wrapper)
        publish_params['Message'] = sns_message
        
        # Intentar envío con retry logic
        max_retries = 3
        for attempt in range(max_retries):
            try:
                response = sns_client.publish(**publish_params)
                logger.info(f"Email enviado a {email} - MessageId: {response.get('MessageId')} - Formato: {message['format']}")
                return  # Éxito, salir del bucle
                
            except Exception as e:
                if attempt == max_retries - 1:
                    # Último intento, propagar la excepción
                    raise e
                else:
                    logger.warning(f"Reintento {attempt + 1} para email {email}: {str(e)}")
                    time.sleep(2 ** attempt)  # Backoff exponencial: 1s, 2s, 4s
        
    except Exception as e:
        logger.error(f"Error enviando email a {email}: {str(e)}")
        raise e
