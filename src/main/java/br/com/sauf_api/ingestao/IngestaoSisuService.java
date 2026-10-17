package br.com.sauf_api.ingestao;

import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.sisu.NotaCorteSisu;
import br.com.sauf_api.sisu.NotaCorteSisuRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngestaoSisuService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoSisuService.class);

    private final InstituicaoRepository instituicoes;
    private final OfertaCursoRepository ofertas;
    private final NotaCorteSisuRepository notas;

    public IngestaoSisuService(
            InstituicaoRepository instituicoes, OfertaCursoRepository ofertas, NotaCorteSisuRepository notas) {
        this.instituicoes = instituicoes;
        this.ofertas = ofertas;
        this.notas = notas;
    }

    /** Cria ou atualiza cada (curso e-MEC, turno, ano). Outros anos não são tocados (D-50). */
    public RespostaSisu importar(LoteSisuRequest lote) {
        RespostaSisu resposta = new RespostaSisu();
        Map<Integer, Instituicao> porCodigo = new HashMap<>();
        Set<Integer> codigos = lote.linhas().stream()
                .map(LoteSisuRequest.Linha::codigoEmecCurso)
                .collect(Collectors.toSet());
        Set<Integer> comOferta = codigos.isEmpty() ? Set.of() : ofertas.codigosEmecComOferta(codigos);

        for (LoteSisuRequest.Linha linha : lote.linhas()) {
            Instituicao instituicao = porCodigo.computeIfAbsent(linha.codigoEmecInstituicao(), this::instituicao);
            Optional<NotaCorteSisu> existente =
                    notas.findByCodigoEmecCursoAndTurnoAndAno(linha.codigoEmecCurso(), linha.turno(), lote.ano());
            NotaCorteSisu nota = existente.orElseGet(
                    () -> new NotaCorteSisu(instituicao, linha.codigoEmecCurso(), linha.turno(), lote.ano()));
            nota.atualizar(linha.vagas(), linha.inscricoes(), linha.nota());
            notas.save(nota);
            if (existente.isPresent()) {
                resposta.atualizado();
            } else {
                resposta.criado();
            }
            if (!comOferta.contains(linha.codigoEmecCurso())) {
                resposta.semOferta();
            }
        }
        log.info("sisu {} importado: {} criados, {} atualizados, {} sem oferta",
                lote.ano(), resposta.getCriados(), resposta.getAtualizados(), resposta.getSemOferta());
        return resposta;
    }

    private Instituicao instituicao(Integer codigoEmec) {
        return instituicoes.findByCodigoEmec(codigoEmec).orElseThrow(() -> {
            log.warn("sisu recusado: instituição {} não cadastrada", codigoEmec);
            return new InstituicaoNaoCadastradaException(codigoEmec);
        });
    }
}
