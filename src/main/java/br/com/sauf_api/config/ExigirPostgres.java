package br.com.sauf_api.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Ao subir a API, exige que o banco seja o PostgreSQL configurado em SAUF_DB_URL.
 * Assim ela nunca roda num banco errado (ex.: em memória) por engano.
 * Os testes não passam por aqui: eles sobem o contexto sem o main().
 */
public class ExigirPostgres implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final String PREFIXO = "jdbc:postgresql://";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        validar(event.getEnvironment());
    }

    static void validar(Environment env) {
        String url;
        try {
            url = env.getProperty("spring.datasource.url");
        } catch (IllegalArgumentException e) {
            url = null;
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "SAUF_DB_URL não está configurada. Preencha o .env (veja o README) com a URL do Supabase, "
                            + "ex.: " + PREFIXO + "host:5432/postgres?sslmode=require");
        }
        if (!url.startsWith(PREFIXO)) {
            throw new IllegalStateException(
                    "SAUF_DB_URL precisa começar com " + PREFIXO + " (a API só roda com PostgreSQL). "
                            + "Confira o .env.");
        }
    }
}
