package br.com.sauf_api.ingestao;

import br.com.sauf_api.area.Area;
import br.com.sauf_api.area.AreaRepository;
import br.com.sauf_api.comum.DadosInvalidosException;
import br.com.sauf_api.imagem.ArquivoImagem;
import br.com.sauf_api.imagem.ImagemInstituicao;
import br.com.sauf_api.imagem.ImagensCatalogo;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados cadastrados à mão no scraper (D-98): ano de fundação, sobre, site, logo e capa da universidade
 * e a imagem de cada área Cine. Cada envio substitui o bloco inteiro; reenviar igual não muda nada.
 */
@Service
public class IngestaoDadosManuaisService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoDadosManuaisService.class);

    private final InstituicaoRepository instituicoes;
    private final AreaRepository areas;
    private final ImagensCatalogo imagens;
    private final Clock relogio;

    public IngestaoDadosManuaisService(
            InstituicaoRepository instituicoes, AreaRepository areas, ImagensCatalogo imagens, Clock relogio) {
        this.instituicoes = instituicoes;
        this.areas = areas;
        this.imagens = imagens;
        this.relogio = relogio;
    }

    @Transactional
    public RespostaIngestao universidade(Integer codigoEmec, DadosManuaisUniversidadeRequest dados) {
        Instituicao instituicao = instituicoes.findByCodigoEmec(codigoEmec)
                .orElseThrow(() -> new InstituicaoNaoCadastradaException(codigoEmec));
        if (dados.anoFundacao() != null && dados.anoFundacao() > Year.now(relogio).getValue()) {
            throw new DadosInvalidosException("anoFundacao", "O ano de fundação não pode estar no futuro");
        }
        List<String> sobre = paragrafos(dados.sobre());
        // Decodifica as duas antes de gravar: imagem inválida não deixa o bloco pela metade
        ArquivoImagem logo = dados.logo() == null ? null : ArquivoImagem.deBase64(dados.logo().conteudoBase64(), "logo");
        ArquivoImagem capa = dados.capa() == null ? null : ArquivoImagem.deBase64(dados.capa().conteudoBase64(), "capa");

        boolean mudou = instituicao.aplicarDadosManuais(dados.anoFundacao(), sobre, vazioComoNull(dados.siteUrl()));
        mudou |= imagens.definirDaUniversidade(instituicao.getId(), ImagemInstituicao.Tipo.LOGO, logo,
                dados.logo() == null ? null : dados.logo().credito());
        mudou |= imagens.definirDaUniversidade(instituicao.getId(), ImagemInstituicao.Tipo.CAPA, capa,
                dados.capa() == null ? null : dados.capa().credito());

        RespostaIngestao resposta = new RespostaIngestao();
        if (mudou) {
            resposta.atualizado();
        }
        log.info("dados manuais da universidade: codigoEmec={} mudou={}", codigoEmec, mudou);
        return resposta;
    }

    @Transactional
    public RespostaIngestao imagemDaArea(String codigoCine, ImagemRequest imagem) {
        Area area = areas.findByCodigoCine(codigoCine)
                .orElseThrow(() -> new AreaNaoCadastradaException(codigoCine));
        ArquivoImagem arquivo = ArquivoImagem.deBase64(imagem.conteudoBase64(), "conteudoBase64");
        RespostaIngestao resposta = new RespostaIngestao();
        if (imagens.definirDaArea(area.getId(), arquivo, imagem.credito())) {
            resposta.atualizado();
        }
        return resposta;
    }

    // Parágrafo com linha em branco dentro viraria dois ao ler (o banco separa por linha em branco)
    private static List<String> paragrafos(List<String> sobre) {
        if (sobre == null) {
            return List.of();
        }
        List<String> limpos = sobre.stream().map(String::strip).toList();
        for (int i = 0; i < limpos.size(); i++) {
            if (limpos.get(i).contains("\n\n")) {
                throw new DadosInvalidosException("sobre[" + i + "]", "Cada parágrafo vai num item separado da lista");
            }
        }
        return limpos;
    }

    private static String vazioComoNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
