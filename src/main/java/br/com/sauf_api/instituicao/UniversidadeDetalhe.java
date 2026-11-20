package br.com.sauf_api.instituicao;

import br.com.sauf_api.oferta.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UniversidadeDetalhe(
        UUID id,
        Integer codigoEmec,
        String nome,
        String sigla,
        TipoInstituicao tipo,
        @Schema(description = "federal, estadual ou municipal, tirado da categoria administrativa; null para privadas")
        Esfera esfera,
        String categoriaAdministrativa,
        String organizacaoAcademica,
        String cidade,
        String uf,
        @Schema(description = "Endereço da SEDE da IES (Censo), não de cada campus; cidade e UF sempre vêm, "
                + "o resto pode ser null. Ofertas de outros campi mostram só a cidade/UF delas (D-58)")
        EnderecoResposta enderecoSede,
        String siteUrl,
        @Schema(description = "URL completa do logo (Wikimedia Commons, crédito em dados manuais); null sem logo (D-100)") String logoUrl,
        @Schema(description = "URL completa da foto de capa (Wikimedia Commons); null sem capa (D-100)") String capaUrl,
        @Schema(description = "Ano da criação legal da instituição (D-102); null sem dados manuais") Integer anoFundacao,
        @Schema(description = "IGC faixa (1 a 5) do ano mais recente importado; null quando sem conceito (SC) ou sem IGC (D-49)") Integer notaMec,
        @Schema(description = "true se todas as ofertas com essa informação forem gratuitas (dado do Censo)") boolean gratuita,
        @Schema(description = "Soma das vagas das ofertas que têm essa informação")
        Integer vagasPorAno,
        long totalCursos,
        @Schema(description = "Parágrafos escritos pelo time a partir das fontes oficiais (D-103); vazio sem dados manuais") List<String> sobre,
        @Schema(description = "Sempre as 3 modalidades, com disponivel true/false") List<ModalidadeOfertada> modalidades,
        List<AreaCursos> cursosPorArea,
        @Schema(description = "Ainda sem fonte de dados; vem vazio") List<FormaIngresso> formasIngresso,
        @Schema(description = "Ainda sem fonte; vem null") String notaFormasIngresso,
        @Schema(description = "Ainda sem fonte de dados; vem null") ProximoPrazo proximoPrazo,
        Instant atualizadaEm) {

    public record AreaCursos(String area, long totalCursos) {}

    public record ModalidadeOfertada(
            Modalidade modalidade,
            boolean disponivel,
            long totalCursos,
            @Schema(description = "Ainda sem fonte; vem null") String descricao) {}

    public record FormaIngresso(String nome, boolean aceita) {}

    public record ProximoPrazo(String titulo, LocalDate data, String editalUrl) {}

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