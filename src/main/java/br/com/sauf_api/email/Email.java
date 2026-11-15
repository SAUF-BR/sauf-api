package br.com.sauf_api.email;

/** Uma mensagem pronta para enviar: texto puro (para quem não lê HTML) e HTML. */
public record Email(String para, String assunto, String texto, String html) {}
