package br.com.sauf_api.auth;

import br.com.sauf_api.documento.AceiteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CadastroRequest(
        @NotNull(message = "Informe seu nome completo")
        @Pattern(regexp = "\\s*\\S.+\\S\\s*", message = "Informe seu nome completo")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,
        @NotBlank(message = "Informe seu e-mail")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,
        @SenhaValida String senha,
        // Versões dos Termos de Uso e da Política de Privacidade que a tela mostrou (D-93)
        @NotEmpty(message = "Aceite os Termos de Uso e a Política de Privacidade para criar a conta")
        List<@Valid AceiteRequest> aceites) {

    // Espaços nas pontas do e-mail (comuns ao colar) não invalidam o cadastro
    public CadastroRequest {
        email = email == null ? null : email.trim();
    }
}
