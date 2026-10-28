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
        @Schema(description = "Endereço da SEDE da IES (Censo), não de cada campus; cidade e UF sempre vêm, "
                + "o resto pode ser null. Ofertas de outros campi mostram só a cidade/UF delas (D-58)")
                EnderecoResposta enderecoSede,
        String siteUrl,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") String logoUrl,
        @Schema(description = "IGC faixa (1 a 5) do ano mais recente importado; null quando sem conceito (SC) ou sem IGC (D-49)") Integer notaMec,
        long totalCursos,
        Set<Modalidade> modalidades,
        List<AreaCursos> cursosPorArea,
        Instant atualizadaEm) {

    public record AreaCursos(String area, long totalCursos) {}

    public record EnderecoResposta(
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            @Schema(description = "Só os 8 dígitos, ex.: 87020900") String cep,
            String cidade,
            String uf) {

        static EnderecoResposta de(Instituicao i) {
            Endereco e = i.getEndereco();
            return e == null
                    ? new EnderecoResposta(null, null, null, null, null, i.getCidade(), i.getUf())
                    : new EnderecoResposta(
                            e.getLogradouro(), e.getNumero(), e.getComplemento(), e.getBairro(), e.getCep(),
                            i.getCidade(), i.getUf());
        }
    }
}
