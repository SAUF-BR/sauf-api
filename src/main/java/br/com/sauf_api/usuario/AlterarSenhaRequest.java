package br.com.sauf_api.usuario;

import br.com.sauf_api.auth.SenhaValida;

/**
 * Corpo do PUT /api/v1/me/senha (D-91). A nova senha segue a regra da D-86. A senha atual é
 * obrigatória, menos logo depois de entrar pelo código do "esqueci minha senha" (D-90):
 * quem confere isso é o AuthService, porque depende da sessão.
 */
public record AlterarSenhaRequest(String senhaAtual, @SenhaValida String novaSenha) {}
