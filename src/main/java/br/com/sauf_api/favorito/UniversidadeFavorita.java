package br.com.sauf_api.favorito;

import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.instituicao.UniversidadeResumo;
import br.com.sauf_api.oferta.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;

public record UniversidadeFavorita(
        UUID id,
        String nome,
        String sigla,
        TipoInstituicao tipo,
        String cidade,
        String uf,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") String logoUrl,
        @Schema(description = "IGC faixa (1 a 5) do ano mais recente importado; null quando sem conceito (SC) ou sem IGC (D-49)") Integer notaMec,
        long totalCursos,
        Set<Modalidade> modalidades,
        @Schema(description = "Cidades diferentes das ofertas ativas da universidade; 0 quando não tem ofertas") long totalCampi) {

    static UniversidadeFavorita de(UniversidadeResumo r, long totalCampi) {
        return new UniversidadeFavorita(
                r.id(), r.nome(), r.sigla(), r.tipo(), r.cidade(), r.uf(),
                r.logoUrl(), r.notaMec(), r.totalCursos(), r.modalidades(), totalCampi);
    }
}