package br.com.sauf_api.comum;

import java.util.UUID;

/** O que outros alunos podem ver de um usuário (ranking). {@code uf} pode faltar. */
public record PerfilPublico(
        UUID usuarioId,
        String nome,
        String cidade,
        String uf,
        String fotoUrl,
        boolean menorDeIdade,
        boolean aparecerNoRanking) {

    /** "Larissa Campos Silva" → "Larissa S." (D-88). */
    public String nomeAbreviado() {
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0];
        }
        return partes[0] + " " + partes[partes.length - 1].charAt(0) + ".";
    }

    /** Só entra no pódio quem aceitou aparecer e é maior de idade (D-88). */
    public boolean podeAparecerNoPodio() {
        return aparecerNoRanking && !menorDeIdade;
    }
}
