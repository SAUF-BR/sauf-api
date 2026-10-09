package br.com.sauf_api.ingestao;

import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

public record LoteScraperRequest(
        String execucaoId,
        @NotNull @Valid Fonte fonte,
        @NotNull @Positive Integer codigoEmecInstituicao,
        @NotNull List<@Valid Curso> cursos) {

    public record Fonte(@NotBlank String url, Instant coletadoEm, String versaoScraper) {}

    public record Opcao(@NotNull Grau grau, String turno) {}

    public record Curso(
            @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*(:[a-z0-9]+(-[a-z0-9]+)*)*") String chave,
            List<@NotNull @Positive Integer> codigosEmec,
            @NotBlank String urlOrigem,
            @NotBlank String nome,
            List<@Valid Opcao> opcoes,
            Modalidade modalidade,
            String turno,
            String sobre,
            String mercadoTrabalho,
            String duracaoTexto,
            @Positive Integer duracaoSemestres,
            String cidade,
            @Pattern(regexp = "[A-Z]{2}") String uf) {

        public List<Integer> codigosEmecOuVazio() {
            return codigosEmec == null ? List.of() : codigosEmec;
        }

        public List<Opcao> opcoesOuVazio() {
            return opcoes == null ? List.of() : opcoes;
        }
    }
}
