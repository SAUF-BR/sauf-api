package br.com.sauf_api.ingestao;

import br.com.sauf_api.area.Area;
import br.com.sauf_api.area.AreaRepository;
import br.com.sauf_api.censo.CensoCurso;
import br.com.sauf_api.censo.CensoCursoRepository;
import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.curso.CursoRepository;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.Opcao;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngestaoService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoService.class);
    private static final String SUFIXO_LICENCIATURA = " formação de professor";

    private final InstituicaoRepository instituicoes;
    private final AreaRepository areas;
    private final CursoRepository cursos;
    private final CensoCursoRepository censo;
    private final OfertaCursoRepository ofertas;

    public IngestaoService(
            InstituicaoRepository instituicoes,
            AreaRepository areas,
            CursoRepository cursos,
            CensoCursoRepository censo,
            OfertaCursoRepository ofertas) {
        this.instituicoes = instituicoes;
        this.areas = areas;
        this.cursos = cursos;
        this.censo = censo;
        this.ofertas = ofertas;
    }

    /** O Censo não cria ofertas: guarda os cursos dele para completar as ofertas do site (D-33). */
    public RespostaIngestao importarCenso(LoteCensoRequest lote) {
        RespostaIngestao resposta = new RespostaIngestao();
        Map<Integer, Instituicao> porCodigo = new HashMap<>();

        for (LoteCensoRequest.Instituicao dados : lote.instituicoes()) {
            Optional<Instituicao> existente = instituicoes.findByCodigoEmec(dados.codigoEmec());
            Instituicao instituicao = existente.orElseGet(
                    () -> new Instituicao(dados.codigoEmec(), dados.nome(), dados.tipo(), dados.uf(), dados.cidade()));
            instituicao.atualizarDoCenso(
                    dados.nome(),
                    dados.sigla(),
                    dados.tipo(),
                    dados.categoriaAdministrativa(),
                    dados.organizacaoAcademica(),
                    dados.uf(),
                    dados.cidade());
            porCodigo.put(dados.codigoEmec(), instituicoes.save(instituicao));
            contar(resposta, existente.isPresent());
        }

        // D-36: bacharelado antes de "formação de professor", para a área do bacharelado prevalecer.
        List<LoteCensoRequest.Oferta> ordenadas = lote.ofertas().stream()
                .sorted(Comparator.comparing(o -> ehFormacaoDeProfessor(o.nomeCineRotulo())))
                .toList();
        for (LoteCensoRequest.Oferta dados : ordenadas) {
            Instituicao instituicao = porCodigo.computeIfAbsent(
                    dados.codigoEmecInstituicao(), codigo -> instituicoes.findByCodigoEmec(codigo).orElse(null));
            if (instituicao == null) {
                resposta.rejeitado("curso " + dados.codigoEmec() + ": instituição " + dados.codigoEmecInstituicao()
                        + " não cadastrada");
                continue;
            }
            Optional<Area> area = areas.findByCodigoCine(dados.codigoCineArea());
            if (area.isEmpty()) {
                resposta.rejeitado("curso " + dados.codigoEmec() + ": área Cine " + dados.codigoCineArea()
                        + " desconhecida");
                continue;
            }
            Curso curso = cursoGenerico(dados.codigoCineRotulo(), dados.nomeCineRotulo(), area.get());

            Optional<CensoCurso> existente = censo.findById(dados.codigoEmec());
            CensoCurso linha = existente.orElseGet(() -> new CensoCurso(dados.codigoEmec()));
            linha.atualizar(
                    instituicao,
                    curso,
                    dados.nome(),
                    dados.grau(),
                    dados.modalidade(),
                    dados.cidade(),
                    dados.uf(),
                    dados.vagas(),
                    dados.gratuito(),
                    lote.anoCenso());
            censo.save(linha);
            contar(resposta, existente.isPresent());
        }

        // Ofertas do site que já existiam passam a usar os dados novos do Censo.
        for (Instituicao instituicao : porCodigo.values()) {
            if (instituicao != null) {
                ofertas.findByInstituicaoId(instituicao.getId()).forEach(this::completarComCenso);
            }
        }

        log.info("censo {} importado: {} criados, {} atualizados, {} rejeitados",
                lote.anoCenso(), resposta.getCriados(), resposta.getAtualizados(), resposta.getRejeitados().size());
        return resposta;
    }

    public RespostaIngestao importarLote(LoteScraperRequest lote) {
        Instituicao instituicao = instituicoes.findByCodigoEmec(lote.codigoEmecInstituicao()).orElseThrow(() -> {
            log.warn("lote recusado: instituição {} não cadastrada (execução {})",
                    lote.codigoEmecInstituicao(), lote.execucaoId());
            return new InstituicaoNaoCadastradaException(lote.codigoEmecInstituicao());
        });
        Instant coletadoEm = lote.fonte().coletadoEm() != null ? lote.fonte().coletadoEm() : Instant.now();
        RespostaIngestao resposta = new RespostaIngestao();

        for (LoteScraperRequest.Curso dados : lote.cursos()) {
            Optional<OfertaCurso> existente = ofertas.findByInstituicaoIdAndChave(instituicao.getId(), dados.chave());
            OfertaCurso oferta = existente.orElseGet(
                    () -> new OfertaCurso(cursoForaDoCenso(dados.nome()), instituicao, dados.chave(), dados.nome()));
            oferta.setNomeNoSite(dados.nome());
            oferta.setOpcoes(dados.opcoesOuVazio().stream().map(o -> new Opcao(o.grau(), o.turno())).toList());
            oferta.setCodigosEmec(dados.codigosEmecOuVazio());
            oferta.setUrlOrigem(dados.urlOrigem());
            oferta.setModalidade(dados.modalidade());
            oferta.setTurno(dados.turno());
            oferta.setSobre(dados.sobre());
            oferta.setMercadoTrabalho(dados.mercadoTrabalho());
            oferta.setDuracaoTexto(dados.duracaoTexto());
            oferta.setDuracaoSemestres(dados.duracaoSemestres());
            oferta.setCidade(dados.cidade());
            oferta.setUf(dados.uf());
            oferta.marcarColetada(coletadoEm);
            completarComCenso(oferta);
            ofertas.save(oferta);
            contar(resposta, existente.isPresent());
        }

        // Provisório (P-26): oferta que não veio neste lote fica inativa (não é apagada).
        Set<String> chavesDoLote =
                lote.cursos().stream().map(LoteScraperRequest.Curso::chave).collect(Collectors.toSet());
        for (OfertaCurso oferta : ofertas.findByInstituicaoIdAndAtivaTrue(instituicao.getId())) {
            if (!chavesDoLote.contains(oferta.getChave())) {
                oferta.setAtiva(false);
                resposta.inativado();
            }
        }

        log.info("lote {} da instituição {}: {} criados, {} atualizados, {} inativados, {} rejeitados",
                lote.execucaoId(), lote.codigoEmecInstituicao(), resposta.getCriados(), resposta.getAtualizados(),
                resposta.getInativados(), resposta.getRejeitados().size());
        return resposta;
    }

    /**
     * D-35: os códigos e-MEC da página ligam a oferta ao Censo, que completa o curso genérico (e a
     * área), as vagas (soma) e a gratuidade. Sem código ou sem Censo, a oferta fica num curso
     * pendente de revisão (P-05, provisório).
     */
    private void completarComCenso(OfertaCurso oferta) {
        List<CensoCurso> doCenso = censo.findAllById(oferta.getCodigosEmec());
        if (doCenso.isEmpty()) {
            if (!oferta.getCurso().isPendenteRevisao()) {
                oferta.setCurso(cursoForaDoCenso(oferta.getNomeNoSite()));
            }
            oferta.setVagas(null);
            oferta.setGratuito(null);
            return;
        }
        oferta.setCurso(cursoMaisFrequente(doCenso));
        oferta.setVagas(doCenso.stream().map(CensoCurso::getVagas).filter(Objects::nonNull)
                .reduce(Integer::sum).orElse(null));
        List<Boolean> gratuidade = doCenso.stream().map(CensoCurso::getGratuito).filter(Objects::nonNull).toList();
        oferta.setGratuito(gratuidade.isEmpty() ? null : gratuidade.contains(true));
        if (oferta.getOpcoes().isEmpty()) {
            // O site manda (D-33): os graus do Censo só entram quando a página não informa.
            Set<Grau> graus = new HashSet<>();
            doCenso.stream().map(CensoCurso::getGrau).filter(Objects::nonNull).forEach(graus::add);
            oferta.setOpcoes(graus.stream().sorted().map(g -> new Opcao(g, null)).toList());
        }
    }

    private static Curso cursoMaisFrequente(List<CensoCurso> doCenso) {
        Map<Curso, Long> contagem = doCenso.stream()
                .collect(Collectors.groupingBy(CensoCurso::getCurso, Collectors.counting()));
        return contagem.entrySet().stream()
                .max(Map.Entry.<Curso, Long>comparingByValue()
                        .thenComparing(e -> e.getKey().getNome(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    /**
     * D-31 + D-36: o rótulo Cine agrupa as ofertas; "X formação de professor" entra no mesmo curso
     * genérico que "X", e a área do bacharelado prevalece.
     */
    private Curso cursoGenerico(String codigoCineRotulo, String nomeCineRotulo, Area area) {
        Optional<Curso> pelaChave = cursos.findByCodigoCineRotulo(codigoCineRotulo);
        if (pelaChave.isPresent()) {
            return pelaChave.get();
        }
        boolean licenciatura = ehFormacaoDeProfessor(nomeCineRotulo);
        String nome = licenciatura
                ? nomeCineRotulo.substring(0, nomeCineRotulo.length() - SUFIXO_LICENCIATURA.length()).trim()
                : nomeCineRotulo;
        Optional<Curso> peloNome = cursos.findFirstByPendenteRevisaoFalseAndNomeIgnoreCase(nome);
        if (peloNome.isPresent()) {
            if (!licenciatura) {
                peloNome.get().usarAreaDoBacharelado(area);
            }
            return peloNome.get();
        }
        return cursos.save(Curso.doCenso(codigoCineRotulo, nome, area));
    }

    private static boolean ehFormacaoDeProfessor(String nomeCineRotulo) {
        return nomeCineRotulo.toLowerCase(Locale.ROOT).endsWith(SUFIXO_LICENCIATURA);
    }

    // Provisório (P-05): curso fora do Censo vira curso genérico com o nome do site, sem área,
    // marcado para revisão.
    private Curso cursoForaDoCenso(String nomeNoSite) {
        return cursos.findFirstByPendenteRevisaoTrueAndNomeIgnoreCase(nomeNoSite)
                .orElseGet(() -> cursos.save(Curso.pendenteDeRevisao(nomeNoSite)));
    }

    private static void contar(RespostaIngestao resposta, boolean existia) {
        if (existia) {
            resposta.atualizado();
        } else {
            resposta.criado();
        }
    }
}
