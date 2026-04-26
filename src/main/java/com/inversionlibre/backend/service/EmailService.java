package com.inversionlibre.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        log.info("Enviando email de bienvenida a: {}", to);
        String subject = "¡Bienvenido a Inversión Libre, " + firstName + "!";
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden; }
                    .header { background-color: #f59e0b; padding: 40px 20px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 28px; text-transform: uppercase; letter-spacing: 2px; }
                    .content { padding: 40px 30px; background-color: #ffffff; }
                    .content h2 { color: #1f2937; margin-top: 0; }
                    .footer { background-color: #1f2937; color: #9ca3af; padding: 20px; text-align: center; font-size: 12px; }
                    .button { display: inline-block; padding: 12px 25px; background-color: #f59e0b; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
                    .highlight { color: #f59e0b; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Inversión Libre</h1>
                    </div>
                    <div class="content">
                        <h2>¡Hola, %s! 👋</h2>
                        <p>Es un placer darte la bienvenida a <b>Inversión Libre</b>, tu nueva plataforma para dominar tus finanzas y alcanzar la libertad financiera.</p>
                        <p>Estamos aquí para ayudarte a rastrear tu patrimonio, analizar acciones con <span class="highlight">Inteligencia Artificial</span> y planificar tu camino hacia el retiro anticipado (método FIRE).</p>
                        <p>¿Qué puedes hacer ahora?</p>
                        <ul>
                            <li>Configura tus carteras de inversión.</li>
                            <li>Habla con nuestro <b>Asistente IA</b> para resolver dudas.</li>
                            <li>Establece tus objetivos de ahorro.</li>
                        </ul>
                        <a href="%s" class="button">Entrar a mi Dashboard</a>
                        <p style="margin-top: 30px;">¡Nos vemos dentro!</p>
                        <p>El equipo de Inversión Libre.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 Inversión Libre - Proyecto TFG</p>
                        <p>Has recibido este correo porque te has registrado en nuestra plataforma.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, frontendUrl);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendPriceAlertEmail(String to, String firstName, String symbol, String symbolName, String alertType, java.math.BigDecimal targetPrice, java.math.BigDecimal currentPrice) {
        log.info("Enviando alerta de precio a: {} para {}", to, symbol);
        String subject = "🔔 ¡Alerta de Precio alcanzada!: " + symbol;
        
        String conditionText = switch (alertType) {
            case "STOP_LOSS" -> "ha caído por debajo de";
            case "TAKE_PROFIT" -> "ha superado";
            case "PRICE_TARGET" -> "ha alcanzado";
            default -> "ha llegado a";
        };

        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; border: 1px solid #e5e7eb; border-radius: 10px; overflow: hidden; }
                    .header { background-color: #000000; color: #f59e0b; padding: 30px; text-align: center; border-bottom: 4px solid #f59e0b; }
                    .content { padding: 40px; background-color: #ffffff; }
                    .alert-box { background-color: #fffbeb; border: 2px solid #f59e0b; border-radius: 15px; padding: 25px; text-align: center; margin: 20px 0; }
                    .symbol { font-size: 24px; font-weight: 800; color: #1f2937; margin-bottom: 5px; }
                    .price { font-size: 36px; font-weight: 900; color: #f59e0b; }
                    .footer { background-color: #1f2937; color: #9ca3af; padding: 20px; text-align: center; font-size: 12px; }
                    .button { display: inline-block; padding: 15px 30px; background-color: #f59e0b; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; margin-top: 25px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Alerta de Inversión</h1>
                    </div>
                    <div class="content">
                        <h2>¡Hola, %s!</h2>
                        <p>Tu alerta para <b>%s</b> (%s) se ha disparado.</p>
                        
                        <div class="alert-box">
                            <div class="symbol">%s</div>
                            <p>El precio %s tu objetivo de <span style="font-weight: bold;">$%s</span></p>
                            <div class="price">$%s</div>
                        </div>

                        <p>Es un buen momento para revisar tu estrategia y decidir si quieres comprar, vender o mantener tu posición.</p>
                        
                        <div style="text-align: center;">
                            <a href="%s/dashboard/stocks/%s" class="button">Ver detalles en el Dashboard</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 Inversión Libre - Tu asistente financiero</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                firstName, 
                symbolName != null ? symbolName : symbol, 
                symbol, 
                symbol, 
                conditionText, 
                targetPrice.toPlainString(), 
                currentPrice.toPlainString(),
                frontendUrl,
                symbol
            );

        sendHtmlEmail(to, subject, template);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("Inversión Libre <info@inversionlibre.es>");
            
            mailSender.send(message);
            log.info("Email enviado con éxito a {}", to);
        } catch (MessagingException e) {
            log.error("Fallo al enviar email a {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al enviar email: ", e);
        }
    }
}
