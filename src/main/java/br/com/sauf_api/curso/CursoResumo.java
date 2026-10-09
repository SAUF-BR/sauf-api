package br.com.sauf_api.curso;

import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record CursoResumo(
        UUID id,
        String nome,
        AreaRef area,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") String imagemUrl,
        long totalUniversidades,
        Set<Modalidade> modalidades,
        Set<Grau> graus,
        Integer duracaoSemestresMin,
        Integer duracaoSemestresMax,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") BigDecimal notaCorteSisuMin) {

    public record AreaRef(UUID id, String nome) {}

    public static CursoResumo de(Curso c, EstatisticasOfertas.PorCurso numeros) {
        return new CursoResumo(
                c.getId(),
                c.getNome(),
                c.getArea() == null ? null : new AreaRef(c.getArea().getId(), c.getArea().getNome()),
                c.getImagemUrl(),
                numeros.totalUniversidades(),
                numeros.modalidades(),
                numeros.graus(),
                numeros.duracaoSemestresMin(),
                numeros.duracaoSemestresMax(),
                numeros.notaCorteSisuMin());
    }
}
