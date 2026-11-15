package br.com.sauf_api.email;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Textos dos e-mails da conta (D-90). */
@Component
public class ModelosEmail {

    /** Código de 6 dígitos do "esqueci minha senha": a pessoa digita na tela e entra na conta. */
    public Email codigoAcesso(String para, String nome, String codigo) {
        String primeiroNome = nome.trim().split("\\s+")[0];
        String assunto = "Seu código de acesso ao SAUF: " + codigo;
        String intro = "Recebemos um pedido para entrar na sua conta do SAUF porque você esqueceu a senha. "
                + "Digite este código na tela do SAUF:";
        String rodape = "O código vale por 10 minutos e pode ser usado uma vez. Depois de entrar, crie uma "
                + "senha nova no seu perfil. Se você não pediu, ignore este e-mail: ninguém entra na sua "
                + "conta sem este código.";

        String texto = """
                Olá, %s!

                %s

                %s

                %s

                Equipe SAUF
                """.formatted(primeiroNome, intro, codigo, rodape);

        String html = """
                <!doctype html>
                <html lang="pt-BR">
                <body style="margin:0;padding:24px;background:#F5F1E8;font-family:Arial,Helvetica,sans-serif;color:#1f2937">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                    <tr><td align="center">
                      <table role="presentation" width="480" cellspacing="0" cellpadding="0"
                             style="max-width:480px;background:#FFFFFF;border-radius:16px;padding:32px">
                        <tr><td style="font-size:22px;font-weight:bold;color:#204B57">SAUF<span style="color:#E2B93B">.</span>BR</td></tr>
                        <tr><td style="padding-top:24px;font-size:16px">Olá, %s!</td></tr>
                        <tr><td style="padding-top:12px;font-size:15px;line-height:1.5">%s</td></tr>
                        <tr><td align="center" style="padding:28px 0">
                          <span style="display:inline-block;padding:16px 28px;background:#E3EEE8;color:#1F5E4B;
                                border-radius:12px;font-size:32px;font-weight:bold;letter-spacing:8px;
                                font-family:'Courier New',monospace">%s</span>
                        </td></tr>
                        <tr><td style="font-size:13px;color:#6F7A77;line-height:1.5">%s</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        HtmlUtils.htmlEscape(primeiroNome),
                        HtmlUtils.htmlEscape(intro),
                        HtmlUtils.htmlEscape(codigo),
                        HtmlUtils.htmlEscape(rodape));
        return new Email(para, assunto, texto, html);
    }
}
