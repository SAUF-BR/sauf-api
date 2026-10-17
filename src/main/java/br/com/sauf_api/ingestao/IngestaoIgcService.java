package br.com.sauf_api.ingestao;

import br.com.sauf_api.instituicao.IndicadorIes;
import br.com.sauf_api.instituicao.IndicadorIesRepository;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngestaoIgcService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoIgcService.class);

    private final InstituicaoRepository instituicoes;
    private final IndicadorIesRepository indicadores;

    public IngestaoIgcService(InstituicaoRepository instituicoes, IndicadorIesRepository indicadores) {
        this.instituicoes = instituicoes;
        this.indicadores = indicadores;
    }

    /**
     * Guarda o IGC do ano (cria ou atualiza o registro daquele ano) e copia para
     * {@code instituicao.notaMec} a faixa do ano mais recente que existir no banco (D-49).
     */
    public RespostaIngestao importar(LoteIgcRequest lote) {
        RespostaIngestao resposta = new RespostaIngestao();
        for (LoteIgcRequest.Instituicao dados : lote.instituicoes()) {
            Instituicao instituicao = instituicoes.findByCodigoEmec(dados.codigoEmec()).orElseThrow(() -> {
                log.warn("igc {} recusado: instituição {} não cadastrada", lote.ano(), dados.codigoEmec());
                return new InstituicaoNaoCadastradaException(dados.codigoEmec());
            });
            Optional<IndicadorIes> existente = indicadores.findByInstituicaoIdAndAno(instituicao.getId(), lote.ano());
            IndicadorIes indicador = existente.orElseGet(() -> new IndicadorIes(instituicao, lote.ano()));
            indicador.atualizarIgc(dados.igcContinuo(), dados.igcFaixa());
            indicadores.saveAndFlush(indicador);
            if (existente.isPresent()) {
                resposta.atualizado();
            } else {
                resposta.criado();
            }

            indicadores.findFirstByInstituicaoIdOrderByAnoDesc(instituicao.getId())
                    .ifPresent(maisRecente -> instituicao.setNotaMec(maisRecente.getIgcFaixa()));
        }
        log.info("igc {} importado: {} criados, {} atualizados",
                lote.ano(), resposta.getCriados(), resposta.getAtualizados());
        return resposta;
    }
}
