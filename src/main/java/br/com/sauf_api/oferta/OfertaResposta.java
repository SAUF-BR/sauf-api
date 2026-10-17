package br.com.sauf_api.oferta;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.sisu.NotasSisu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

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
        @Schema(description = "Menor nota de corte do Sisu (ampla concorrência) entre os cursos e-MEC e turnos da oferta, no ano mais recente com nota; null = sem Sisu, ingresso por vestibular próprio (D-50)")
                BigDecimal notaCorteSisu,
        @Schema(description = "Turno do menor corte (ex.: Integral)") String notaCorteSisuTurno,
        @Schema(description = "Ano (edição do Sisu) do corte") Integer notaCorteSisuAno,
        @Schema(description = "Ainda sem fonte de dados; vem null (P-11)") BigDecimal mensalidade,
        Instant coletadaEm) {

    public record CursoRef(UUID id, String nome) {}

    public record UniversidadeRef(UUID id, String nome, String sigla, TipoInstituicao tipo) {}

    public record OpcaoResposta(Grau grau, String turno) {}

    /** Página de ofertas com o menor corte do Sisu de cada uma, numa consulta só. */
    public static Pagina<OfertaResposta> pagina(Page<OfertaCurso> pagina, NotasSisu sisu) {
        Map<UUID, Set<Integer>> codigos = pagina.getContent().stream()
                .collect(Collectors.toMap(OfertaCurso::getId, OfertaCurso::getCodigosEmec));
        Map<UUID, NotasSisu.Corte> cortes = sisu.menorCorte(codigos);
        return Pagina.de(pagina, o -> de(o, cortes.get(o.getId())));
    }

    public static OfertaResposta de(OfertaCurso oferta, NotasSisu.Corte corte) {
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
                corte == null ? null : corte.nota(),
                corte == null ? null : corte.turno(),
                corte == null ? null : corte.ano(),
                oferta.getMensalidade(),
                oferta.getColetadaEm());
    }
}
