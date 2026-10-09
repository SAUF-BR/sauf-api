package br.com.sauf_api.instituicao;

import br.com.sauf_api.oferta.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UniversidadeDetalhe(
        UUID id,
        Integer codigoEmec,
        String nome,
        String sigla,
        TipoInstituicao tipo,
        String categoriaAdministrativa,
        String organizacaoAcademica,
        String cidade,
        String uf,
        String siteUrl,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") String logoUrl,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") Integer notaMec,
        long totalCursos,
        Set<Modalidade> modalidades,
        List<AreaCursos> cursosPorArea,
        Instant atualizadaEm) {

    public record AreaCursos(String area, long totalCursos) {}
}
