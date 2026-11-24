package br.com.sauf_api.email;

/**
 * Envia e-mails (D-88). A implementação real usa SMTP (SmtpEnvioEmail); os testes trocam
 * por uma caixa em memória. Falha de envio não lança exceção: fica no log e a pessoa
 * pode pedir de novo.
 */
public interface EnvioEmail {

    void enviar(Email email);
}
