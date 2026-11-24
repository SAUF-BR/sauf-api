package br.com.sauf_api.usuario;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corpo do PATCH /me. Mesmas regras da tela: nome com 3+ letras, nota opcional de 0 a 1000. */
public record AtualizarPerfilRequest(
        @NotNull(message = "Informe seu nome completo")
        // Sem os espaços das pontas, pelo menos 3 caracteres
        @Pattern(regexp = "\\s*\\S.+\\S\\s*", message = "Informe seu nome completo")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,
        @DecimalMin(value = "0", message = "Informe uma nota entre 0 e 1000")
        @DecimalMax(value = "1000", message = "Informe uma nota entre 0 e 1000")
        @Digits(integer = 4, fraction = 2, message = "Use no máximo 2 casas decimais")
        BigDecimal notaEnem) {}
