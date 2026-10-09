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
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") String logoUrl,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") Integer notaMec,
        long totalCursos,
        Set<Modalidade> modalidades) {

    public static UniversidadeResumo de(Instituicao i, EstatisticasOfertas.PorInstituicao numeros) {
        return new UniversidadeResumo(
                i.getId(),
                i.getNome(),
                i.getSigla(),
                i.getTipo(),
                i.getCidade(),
                i.getUf(),
                i.getLogoUrl(),
                i.getNotaMec(),
                numeros.totalCursos(),
                numeros.modalidades());
    }
}
