package br.com.sauf_api.instituicao;

import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;

public record UniversidadeResumo(
        UUID id,
        String nome,
        String sigla,
        TipoInstituicao tipo,
        String cidade,
        String uf,
        @Schema(description = "URL completa do logo (Wikimedia Commons, crédito em dados manuais); null sem logo (D-100)") String logoUrl,
        @Schema(description = "IGC faixa (1 a 5) do ano mais recente importado; null quando sem conceito (SC) ou sem IGC (D-49)") Integer notaMec,
        long totalCursos,
        Set<Modalidade> modalidades) {

    public static UniversidadeResumo de(Instituicao i, EstatisticasOfertas.PorInstituicao numeros, String logoUrl) {
        return new UniversidadeResumo(
                i.getId(),
                i.getNome(),
                i.getSigla(),
                i.getTipo(),
                i.getCidade(),
                i.getUf(),
                logoUrl,
                i.getNotaMec(),
                numeros.totalCursos(),
                numeros.modalidades());
    }
}
