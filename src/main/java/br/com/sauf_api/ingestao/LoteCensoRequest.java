package br.com.sauf_api.ingestao;

import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record LoteCensoRequest(
        @NotNull Integer anoCenso,
        @NotNull List<@Valid Instituicao> instituicoes,
        @NotNull List<@Valid Oferta> ofertas) {

    public record Instituicao(
            @NotNull @Positive Integer codigoEmec,
            @NotBlank String nome,
            String sigla,
            @NotNull TipoInstituicao tipo,
            String categoriaAdministrativa,
            String organizacaoAcademica,
            @NotNull @Pattern(regexp = "[A-Z]{2}") String uf,
            @NotBlank String cidade) {}

    public record Oferta(
            @NotNull @Positive Integer codigoEmec,
            @NotNull @Positive Integer codigoEmecInstituicao,
            @NotBlank String nome,
            Grau grau,
            Modalidade modalidade,
            String cidade,
            @Pattern(regexp = "[A-Z]{2}") String uf,
            @PositiveOrZero Integer vagas,
            Boolean gratuito,
            @NotBlank String codigoCineRotulo,
            @NotBlank String nomeCineRotulo,
            @NotNull @Pattern(regexp = "\\d{2}") String codigoCineArea) {}
}
