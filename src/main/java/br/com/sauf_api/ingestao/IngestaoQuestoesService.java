package br.com.sauf_api.ingestao;

import br.com.sauf_api.questao.Alternativa;
import br.com.sauf_api.questao.Lingua;
import br.com.sauf_api.questao.Prova;
import br.com.sauf_api.questao.ProvaRepository;
import br.com.sauf_api.questao.Questao;
import br.com.sauf_api.questao.QuestaoRepository;
import br.com.sauf_api.questao.TextoBase;
import br.com.sauf_api.questao.TextoBaseRepository;
import br.com.sauf_api.questao.TipoQuestao;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IngestaoQuestoesService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoQuestoesService.class);

    private final ProvaRepository provas;
    private final TextoBaseRepository textos;
    private final QuestaoRepository questoes;

    public IngestaoQuestoesService(ProvaRepository provas, TextoBaseRepository textos, QuestaoRepository questoes) {
        this.provas = provas;
        this.textos = textos;
        this.questoes = questoes;
    }

    /**
     * Cria ou atualiza a prova e cada questão por (prova, número, língua). Reenviar troca tudo,
     * inclusive as alternativas. Questão que não passa nas regras do gabarito vai para rejeitados
     * e não é gravada; questões da prova ausentes do lote ficam como estão (D-68).
     */
    public RespostaIngestao importar(LoteQuestoesRequest lote) {
        RespostaIngestao resposta = new RespostaIngestao();
        Prova prova = prova(lote.prova());

        Map<String, Questao> existentes = new HashMap<>();
        for (Questao questao : questoes.findByProva(prova)) {
            existentes.put(chave(questao.getNumero(), questao.getLingua()), questao);
        }
        Map<String, TextoBase> textosDoLote = new HashMap<>();
        Set<String> vistas = new HashSet<>();

        for (LoteQuestoesRequest.Questao dados : lote.questoes()) {
            String chave = chave(dados.numero(), dados.lingua());
            String erro = vistas.add(chave) ? validar(dados) : "aparece mais de uma vez no lote";
            if (erro != null) {
                String motivo = "questão " + chave + ": " + erro;
                log.warn("questão recusada: {}", motivo);
                resposta.rejeitado(motivo);
                continue;
            }
            Questao questao = existentes.get(chave);
            if (questao == null) {
                questao = new Questao(prova, dados.numero(), dados.lingua());
                resposta.criado();
            } else {
                resposta.atualizado();
            }
            questao.atualizar(conteudo(dados, textoBase(prova, dados.textoBase(), textosDoLote)));
            questoes.save(questao);
        }
        questoes.flush();
        int apagados = textos.apagarSemQuestao(prova);

        log.info("questões {} {} {} {}: {} criadas, {} atualizadas, {} recusadas, {} textos-base sem uso apagados",
                prova.getBanca(), prova.getAno(), prova.getEdicao(), prova.getCaderno(),
                resposta.getCriados(), resposta.getAtualizados(), resposta.getRejeitados().size(), apagados);
        return resposta;
    }

    /** Múltipla escolha: exatamente 1 correta (nenhuma se anulada). Somatória: a soma das corretas fecha. */
    static String validar(LoteQuestoesRequest.Questao dados) {
        List<LoteQuestoesRequest.Alternativa> alternativas = dados.alternativas();
        Set<String> rotulos = new HashSet<>();
        for (LoteQuestoesRequest.Alternativa alternativa : alternativas) {
            if (!rotulos.add(alternativa.rotulo())) {
                return "alternativa " + alternativa.rotulo() + " repetida";
            }
        }
        long corretas = alternativas.stream().filter(LoteQuestoesRequest.Alternativa::correta).count();
        if (dados.tipo() == TipoQuestao.MULTIPLA_ESCOLHA) {
            if (dados.anulada()) {
                return corretas == 0 ? null : "anulada não pode ter alternativa correta";
            }
            return corretas == 1 ? null : "múltipla escolha precisa de exatamente 1 correta (veio " + corretas + ")";
        }
        if (dados.anulada()) {
            return null;
        }
        if (alternativas.stream().anyMatch(a -> a.valor() == null)) {
            return "somatória precisa do valor em todas as alternativas";
        }
        int soma = alternativas.stream()
                .filter(LoteQuestoesRequest.Alternativa::correta)
                .mapToInt(LoteQuestoesRequest.Alternativa::valor)
                .sum();
        return Objects.equals(dados.gabaritoSoma(), soma)
                ? null
                : "gabaritoSoma " + dados.gabaritoSoma() + " diferente da soma das corretas (" + soma + ")";
    }

    private Prova prova(LoteQuestoesRequest.Prova dados) {
        Prova prova = provas.findByBancaAndAnoAndEdicaoAndCaderno(
                        dados.banca(), dados.ano(), dados.edicao(), dados.caderno())
                .orElseGet(() -> new Prova(dados.banca(), dados.ano(), dados.edicao(), dados.caderno()));
        prova.atualizar(dados.urlOrigem(), dados.credito());
        return provas.save(prova);
    }

    private TextoBase textoBase(Prova prova, String conteudo, Map<String, TextoBase> textosDoLote) {
        if (conteudo == null || conteudo.isBlank()) {
            return null;
        }
        String hash = sha256(conteudo);
        return textosDoLote.computeIfAbsent(hash, h -> textos.findByProvaAndHash(prova, h)
                .orElseGet(() -> textos.save(new TextoBase(prova, conteudo, h))));
    }

    private static Questao.Conteudo conteudo(LoteQuestoesRequest.Questao dados, TextoBase textoBase) {
        List<Alternativa> alternativas = dados.alternativas().stream()
                .map(a -> new Alternativa(a.rotulo(), a.valor(), a.texto(), a.correta()))
                .toList();
        return new Questao.Conteudo(
                textoBase, dados.tipo(), dados.area(), dados.materia(), dados.assunto(), dados.habilidade(),
                dados.comando(), dados.gabaritoSoma(), dados.dificuldade(), dados.anulada(), dados.status(),
                dados.motivoPendencia(), dados.codigoOrigem(), dados.credito(), alternativas);
    }

    private static String chave(Integer numero, Lingua lingua) {
        return lingua == null ? String.valueOf(numero) : numero + " (" + lingua.valor() + ")";
    }

    private static String sha256(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
