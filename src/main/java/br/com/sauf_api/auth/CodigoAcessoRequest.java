package br.com.sauf_api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Corpo do POST /auth/esqueci-senha/codigo (D-90): o e-mail e o código que chegou nele. */
public record CodigoAcessoRequest(
        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "Informe um e-mail válido")
        String email,
        @NotBlank(message = "Informe o código")
        @Pattern(regexp = "\\s*\\d{6}\\s*", message = "O código tem 6 números")
        String codigo) {

    public CodigoAcessoRequest {
        email = email == null ? null : email.trim();
    }
}
