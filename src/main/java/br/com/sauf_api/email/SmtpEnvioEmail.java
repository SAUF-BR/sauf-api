package br.com.sauf_api.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio por SMTP (D-88). Local: Mailpit do docker-compose (localhost:1025, caixa em
 * http://localhost:8025). Produção: qualquer SMTP configurado nas variáveis SAUF_MAIL_*.
 */
@Component
public class SmtpEnvioEmail implements EnvioEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpEnvioEmail.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpEnvioEmail(JavaMailSender mailSender, @Value("${sauf.mail.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviar(Email email) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(email.para());
            helper.setSubject(email.assunto());
            helper.setText(email.texto(), email.html());
            mailSender.send(mensagem);
        } catch (MailException | MessagingException e) {
            log.error("Não foi possível enviar o e-mail \"{}\" para {}", email.assunto(), email.para(), e);
        }
    }
}
