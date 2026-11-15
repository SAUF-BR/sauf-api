package br.com.sauf_api.auth;

/** Resposta do POST /auth/email-disponivel (D-94): false = já existe uma conta com o e-mail. */
public record EmailDisponivelResposta(boolean disponivel) {}
