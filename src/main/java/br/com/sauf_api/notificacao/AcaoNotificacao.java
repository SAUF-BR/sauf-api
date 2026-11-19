package br.com.sauf_api.notificacao;

/** Link interno que a notificação oferece (ex.: "Ver curso" para "/cursos/abc"). */
public record AcaoNotificacao(String rotulo, String para) {}
