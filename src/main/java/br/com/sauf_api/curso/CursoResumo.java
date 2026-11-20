package br.com.sauf_api.curso;

import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.sisu.NotasSisu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record CursoResumo(
        UUID id,
        String nome,
        AreaRef area,
        @Schema(description = "URL completa da imagem: a do curso, se houver, senão a da área Cine (D-101); null sem as duas") String imagemUrl,
        long totalUniversidades,
        Set<Modalidade> modalidades,
        Set<Grau> graus,
        Integer duracaoSemestresMin,
        Integer duracaoSemestresMax,
        @Schema(description = "Média simples das notas de corte do Sisu (ampla concorrência) das ofertas, cada par (curso e-MEC, turno) uma vez, no ano mais recente com nota; null = nenhuma oferta com Sisu (D-50, D-53)")
                BigDecimal notaCorteSisuMedia,
        @Schema(description = "Ano (edição do Sisu) da média") Integer notaCorteSisuAno) {

    public record AreaRef(UUID id, String nome) {}

    public static CursoResumo de(
            Curso c, EstatisticasOfertas.PorCurso numeros, NotasSisu.Media media, String imagemDaArea) {
        return new CursoResumo(
                c.getId(),
                c.getNome(),
                c.getArea() == null ? null : new AreaRef(c.getArea().getId(), c.getArea().getNome()),
                c.getImagemUrl() != null ? c.getImagemUrl() : imagemDaArea,
                numeros.totalUniversidades(),
                numeros.modalidades(),
                numeros.graus(),
                numeros.duracaoSemestresMin(),
                numeros.duracaoSemestresMax(),
                media == null ? null : media.nota(),
                media == null ? null : media.ano());
    }
}
