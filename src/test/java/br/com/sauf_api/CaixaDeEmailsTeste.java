package br.com.sauf_api;

import br.com.sauf_api.email.Email;
import br.com.sauf_api.email.EnvioEmail;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Troca o SMTP por uma caixa em memória nos testes: guarda os e-mails "enviados". */
class CaixaDeEmailsTeste implements EnvioEmail {

    private static final Pattern CODIGO = Pattern.compile("(?m)^\\s*(\\d{6})\\s*$");

    private final List<Email> enviados = new ArrayList<>();

    @Override
    public synchronized void enviar(Email email) {
        enviados.add(email);
    }

    synchronized List<Email> enviados() {
        return List.copyOf(enviados);
    }

    synchronized Email ultimo() {
        return enviados.getLast();
    }

    synchronized void limpar() {
        enviados.clear();
    }

    /** Código de 6 dígitos do último e-mail (fica sozinho numa linha do texto). */
    String codigoDoUltimo() {
        Matcher matcher = CODIGO.matcher(ultimo().texto());
        if (!matcher.find()) throw new AssertionError("E-mail sem código");
        return matcher.group(1);
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        CaixaDeEmailsTeste caixaDeEmails() {
            return new CaixaDeEmailsTeste();
        }
    }
}
