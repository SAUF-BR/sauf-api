package br.com.sauf_api.notificacao;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o {@code @Scheduled} do gerador fora dos testes: assim os testes chamam o
 * gerador diretamente sem risco de ele disparar no fundo e sujar as asserções.
 */
@Configuration
@EnableScheduling
@Profile("!test")
class AgendamentoNotificacoesConfig {}
