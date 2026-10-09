package br.com.sauf_api.oferta;

import br.com.sauf_api.instituicao.TipoInstituicao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfertaResposta(
        UUID id,
        CursoRef curso,
        UniversidadeRef universidade,
        String nomeNoSite,
        List<OpcaoResposta> opcoes,
        Modalidade modalidade,
        String turno,
        String sobre,
        String mercadoTrabalho,
        String duracaoTexto,
        Integer duracaoSemestres,
        String uf,
        String cidade,
        Integer vagas,
        Boolean gratuito,
        String urlOrigem,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") BigDecimal notaCorteSisu,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") BigDecimal mensalidade,
        Instant coletadaEm) {

    public record CursoRef(UUID id, String nome) {}

    public record UniversidadeRef(UUID id, String nome, String sigla, TipoInstituicao tipo) {}

    public record OpcaoResposta(Grau grau, String turno) {}

    public static OfertaResposta de(OfertaCurso oferta) {
        var curso = oferta.getCurso();
        var instituicao = oferta.getInstituicao();
        return new OfertaResposta(
                oferta.getId(),
                new CursoRef(curso.getId(), curso.getNome()),
                new UniversidadeRef(instituicao.getId(), instituicao.getNome(), instituicao.getSigla(), instituicao.getTipo()),
                oferta.getNomeNoSite(),
                oferta.getOpcoes().stream().map(o -> new OpcaoResposta(o.getGrau(), o.getTurno())).toList(),
                oferta.getModalidade(),
                oferta.getTurno(),
                oferta.getSobre(),
                oferta.getMercadoTrabalho(),
                oferta.getDuracaoTexto(),
                oferta.getDuracaoSemestres(),
                oferta.getUf(),
                oferta.getCidade(),
                oferta.getVagas(),
                oferta.getGratuito(),
                oferta.getUrlOrigem(),
                oferta.getNotaCorteSisu(),
                oferta.getMensalidade(),
                oferta.getColetadaEm());
    }
}
