package br.com.sauf_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de "esqueci minha senha" e de "reenviar o e-mail de confirmação". */
public record EmailRequest(
        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "Informe um e-mail válido")
        String email) {

    public EmailRequest {
        email = email == null ? null : email.trim();
    }
}
