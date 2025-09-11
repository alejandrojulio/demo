import json
import boto3
import logging
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
        logger.info(f"Procesando mensaje: {message_body.get('messageId')}")
        
        # Determinar el tipo de notificación
        event_type = message_body.get('eventType')
        
        if event_type == 'LOAN_APPROVED':
            send_loan_approved_notification(message_body)
        elif event_type == 'LOAN_REJECTED':
            send_loan_rejected_notification(message_body)
        else:
            logger.warning(f"Tipo de evento no reconocido: {event_type}")
    
    except Exception as e:
        logger.error(f"Error procesando mensaje SQS: {str(e)}")
        raise e

def send_loan_approved_notification(message: Dict[str, Any]):
    """
    Envía notificación de préstamo aprobado
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
        subject = f"Crédito Aprobado - Solicitud #{solicitud_id}"
        
        # Mensaje de texto plano (corto)
        text_body = f"""Hola {client_name},

¡Tu crédito ha sido APROBADO!

Solicitud: #{solicitud_id}
Monto: {monto_aprobado}
Tasa: {tasa_interes}
Plazo: {plazo_aprobado} meses
Cuota: {pago_mensual}

Un asesor te contactará pronto.

Atentamente,
CrediYa"""

        # Mensaje HTML (simple, sin estilos)
        html_body = f"""<html>
<body>
<h2>Crédito Aprobado</h2>
<p>Hola {client_name},</p>
<p>Tu solicitud #{solicitud_id} ha sido <b>APROBADA</b>.</p>
<p>Monto: {monto_aprobado}<br>
Tasa: {tasa_interes}<br>
Plazo: {plazo_aprobado} meses<br>
Cuota: {pago_mensual}</p>
<p>Un asesor te contactará pronto.</p>
<p>Atentamente,<br>CrediYa</p>
</body>
</html>"""
        
        send_email_notification(client_email, subject, text_body, html_body)
        
        logger.info(f"Notificación de aprobación enviada a {client_email} para solicitud {solicitud_id}")
    
    except Exception as e:
        logger.error(f"Error enviando notificación de aprobación: {str(e)}")
        raise e

def send_loan_rejected_notification(message: Dict[str, Any]):
    """
    Envía notificación de préstamo rechazado
    """
    try:
        client_email = message.get('clientEmail')
        client_name = message.get('clientName')
        solicitud_id = message.get('solicitudId')
        reason = message.get('reason', 'No especificado')
        
        # Crear el mensaje de email
        subject = f"Solicitud de Crédito #{solicitud_id} - Actualización"
        
        # Mensaje de texto plano (corto)
        text_body = f"""Hola {client_name},

Tu solicitud de crédito #{solicitud_id} no fue aprobada.

Motivo: {reason}

Puedes aplicar nuevamente en 30 días.
Para consultas: (01) 234-5678

Atentamente,
CrediYa"""

        # Mensaje HTML (simple, sin estilos)
        html_body = f"""<html>
<body>
<h2>Solicitud No Aprobada</h2>
<p>Hola {client_name},</p>
<p>Tu solicitud #{solicitud_id} no fue aprobada.</p>
<p><b>Motivo:</b> {reason}</p>
<p>Puedes aplicar nuevamente en 30 días.<br>
Para consultas: (01) 234-5678</p>
<p>Atentamente,<br>CrediYa</p>
</body>
</html>"""
        
        send_email_notification(client_email, subject, text_body, html_body)
        
        logger.info(f"Notificación de rechazo enviada a {client_email} para solicitud {solicitud_id}")
    
    except Exception as e:
        logger.error(f"Error enviando notificación de rechazo: {str(e)}")
        raise e

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
            # Mensaje multipart para servicios que soportan HTML
            sns_message = {
                'default': text_body,  # Fallback para servicios que no soportan HTML
                'email': json.dumps({
                    'text': text_body,
                    'html': html_body,
                    'subject': subject,
                    'from': SENDER_EMAIL
                })
            }
            message_structure = 'json'
        else:
            # Mensaje simple de texto plano
            sns_message = text_body
            message_structure = None
        
        # Publicar en el tópico SNS
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
                }
            }
        }
        
        if message_structure:
            publish_params['Message'] = json.dumps(sns_message)
            publish_params['MessageStructure'] = message_structure
        else:
            publish_params['Message'] = sns_message
        
        response = sns_client.publish(**publish_params)
        
        logger.info(f"Email enviado a {email} - MessageId: {response.get('MessageId')} - Formato: {message['format']}")
        
    except Exception as e:
        logger.error(f"Error enviando email a {email}: {str(e)}")
        raise e
