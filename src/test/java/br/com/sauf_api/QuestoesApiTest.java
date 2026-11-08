package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import br.com.sauf_api.questao.Lingua;
import br.com.sauf_api.questao.Prova;
import br.com.sauf_api.questao.ProvaRepository;
import br.com.sauf_api.questao.Questao;
import br.com.sauf_api.questao.QuestaoRepository;
import br.com.sauf_api.questao.StatusQuestao;
import br.com.sauf_api.questao.TextoBaseRepository;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class QuestoesApiTest {

    static final String PROVA = """
            "prova": {"banca": "enem", "ano": 2024, "edicao": "D1", "caderno": "LARANJA",
                      "urlOrigem": "https://huggingface.co/datasets/maritaca-ai/enem",
                      "credito": "ENEM 2024 (INEP), via maritaca-ai/enem (Apache-2.0)"}
            """;

    static final String ALTERNATIVAS_A = """
            [{"rotulo": "A", "texto": "mudança de comportamento.", "correta": true},
             {"rotulo": "B", "texto": "panorama de conflitos.", "correta": false},
             {"rotulo": "C", "texto": "rotina de isolamento.", "correta": false},
             {"rotulo": "D", "texto": "perspectiva bélica.", "correta": false},
             {"rotulo": "E", "texto": "cenário religioso.", "correta": false}]
            """;

    // Duas de inglês com o mesmo texto-base, a 1 de espanhol (mesmo número) e uma anulada.
    static final String LOTE = """
            {%s, "questoes": [
              {"numero": 1, "lingua": "ingles", "tipo": "multipla_escolha", "area": "lc",
               "habilidade": 5, "dificuldade": 0.82582, "codigoOrigem": 141283,
               "textoBase": "## Holy War\\nOh, so we can hate each other", "comando": "O marcador introduz a ideia de",
               "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)", "alternativas": %s},
              {"numero": 2, "lingua": "ingles", "tipo": "multipla_escolha", "area": "lc",
               "textoBase": "## Holy War\\nOh, so we can hate each other", "comando": "A letra critica",
               "anulada": false, "status": "pendente_revisao", "motivoPendencia": "fórmula escrita por extenso",
               "credito": "ENEM 2024 (INEP)", "alternativas": %s},
              {"numero": 1, "lingua": "espanhol", "tipo": "multipla_escolha", "area": "lc",
               "comando": "Según el texto", "anulada": false, "status": "disponivel",
               "credito": "ENEM 2024 (INEP)", "alternativas": %s},
              {"numero": 30, "tipo": "multipla_escolha", "area": "ch", "comando": "Questão anulada",
               "anulada": true, "status": "disponivel", "credito": "ENEM 2024 (INEP)",
               "alternativas": [{"rotulo": "A", "texto": "a", "correta": false},
                                {"rotulo": "B", "texto": "b", "correta": false}]}
            ]}
            """.formatted(PROVA, ALTERNATIVAS_A, ALTERNATIVAS_A, ALTERNATIVAS_A);

    @Autowired MockMvc mvc;
    @Autowired ProvaRepository provas;
    @Autowired QuestaoRepository questoes;
    @Autowired TextoBaseRepository textos;

    @Test
    void criaProvaQuestoesEAlternativasEReaproveitaOTextoBase() throws Exception {
        enviar(LOTE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(4))
                .andExpect(jsonPath("$.atualizados").value(0))
                .andExpect(jsonPath("$.rejeitados").isEmpty());

        Prova prova = prova();
        List<Questao> lista = ordenadas(prova);
        assertThat(lista).hasSize(4);
        Questao ingles = lista.get(0);
        assertThat(ingles.getLingua()).isEqualTo(Lingua.INGLES);
        assertThat(ingles.getArea()).isEqualTo(AreaQuestao.LC);
        assertThat(ingles.getCodigoOrigem()).isEqualTo(141283);
        assertThat(ingles.getAlternativas()).hasSize(5);
        assertThat(ingles.getAlternativas().get(0).isCorreta()).isTrue();
        assertThat(ingles.getTextoBase().getConteudo()).startsWith("## Holy War");
        assertThat(lista.get(1).getLingua()).isEqualTo(Lingua.ESPANHOL);
        assertThat(lista.get(1).getTextoBase()).isNull();
        assertThat(lista.get(2).getTextoBase().getId()).isEqualTo(ingles.getTextoBase().getId());
        assertThat(lista.get(2).getStatus()).isEqualTo(StatusQuestao.PENDENTE_REVISAO);
        assertThat(lista.get(3).getLingua()).isNull();
        assertThat(lista.get(3).isAnulada()).isTrue();
        assertThat(textos.countByProva(prova)).isEqualTo(1);
    }

    @Test
    void reenviarAtualizaTrocaAlternativasEApagaTextoSemUso() throws Exception {
        enviar(LOTE);
        String novo = """
                {%s, "questoes": [
                  {"numero": 1, "lingua": "ingles", "tipo": "multipla_escolha", "area": "lc",
                   "textoBase": "Texto revisado", "comando": "Comando revisado",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)",
                   "alternativas": [{"rotulo": "A", "texto": "x", "correta": false},
                                    {"rotulo": "B", "texto": "y", "correta": true}]},
                  {"numero": 2, "lingua": "ingles", "tipo": "multipla_escolha", "area": "lc",
                   "textoBase": "Texto revisado", "comando": "A letra critica",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)", "alternativas": %s}
                ]}
                """.formatted(PROVA, ALTERNATIVAS_A);

        enviar(novo)
                .andExpect(jsonPath("$.criados").value(0))
                .andExpect(jsonPath("$.atualizados").value(2));

        Prova prova = prova();
        Questao ingles = ordenadas(prova).get(0);
        assertThat(ingles.getComando()).isEqualTo("Comando revisado");
        assertThat(ingles.getAlternativas()).extracting("rotulo").containsExactly("A", "B");
        assertThat(ingles.getAlternativas().get(1).isCorreta()).isTrue();
        assertThat(ingles.getTextoBase().getConteudo()).isEqualTo("Texto revisado");
        assertThat(questoes.findByProva(prova)).hasSize(4);
        assertThat(textos.countByProva(prova)).isEqualTo(1);
    }

    @Test
    void recusaQuestaoComGabaritoInvalidoESegueComAsOutras() throws Exception {
        String lote = """
                {%s, "questoes": [
                  {"numero": 10, "tipo": "multipla_escolha", "area": "ch", "comando": "Duas corretas",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)",
                   "alternativas": [{"rotulo": "A", "texto": "a", "correta": true},
                                    {"rotulo": "B", "texto": "b", "correta": true}]},
                  {"numero": 11, "tipo": "multipla_escolha", "area": "ch", "comando": "Nenhuma correta",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)",
                   "alternativas": [{"rotulo": "A", "texto": "a", "correta": false}]},
                  {"numero": 12, "tipo": "multipla_escolha", "area": "ch", "comando": "Ok",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)", "alternativas": %s},
                  {"numero": 12, "tipo": "multipla_escolha", "area": "ch", "comando": "Repetida",
                   "anulada": false, "status": "disponivel", "credito": "ENEM 2024 (INEP)", "alternativas": %s}
                ]}
                """.formatted(PROVA, ALTERNATIVAS_A, ALTERNATIVAS_A);

        enviar(lote)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(1))
                .andExpect(jsonPath("$.rejeitados.length()").value(3))
                .andExpect(jsonPath("$.rejeitados[0]").value("questão 10: múltipla escolha precisa de exatamente 1 correta (veio 2)"))
                .andExpect(jsonPath("$.rejeitados[2]").value("questão 12: aparece mais de uma vez no lote"));
    }

    @Test
    void somatoriaConfereASomaDasCorretas() throws Exception {
        String uem = """
                {"prova": {"banca": "uem", "ano": 2025, "edicao": "VERAO", "caderno": "P1", "credito": "UEM/CVU"},
                 "questoes": [
                  {"numero": 1, "tipo": "somatoria", "area": "lc", "comando": "Assinale o que for correto.",
                   "gabaritoSoma": 27, "anulada": false, "status": "disponivel", "credito": "UEM/CVU",
                   "alternativas": [{"rotulo": "01", "valor": 1, "texto": "a", "correta": true},
                                    {"rotulo": "02", "valor": 2, "texto": "b", "correta": true},
                                    {"rotulo": "04", "valor": 4, "texto": "c", "correta": false},
                                    {"rotulo": "08", "valor": 8, "texto": "d", "correta": true},
                                    {"rotulo": "16", "valor": 16, "texto": "e", "correta": true}]},
                  {"numero": 2, "tipo": "somatoria", "area": "lc", "comando": "Soma errada",
                   "gabaritoSoma": 3, "anulada": false, "status": "disponivel", "credito": "UEM/CVU",
                   "alternativas": [{"rotulo": "01", "valor": 1, "texto": "a", "correta": true},
                                    {"rotulo": "02", "valor": 2, "texto": "b", "correta": false}]}
                 ]}
                """;

        enviar(uem)
                .andExpect(jsonPath("$.criados").value(1))
                .andExpect(jsonPath("$.rejeitados[0]").value("questão 2: gabaritoSoma 3 diferente da soma das corretas (1)"));
    }

    @Test
    void corpoInvalidoDa400() throws Exception {
        enviar("""
                {%s, "questoes": [{"numero": 1, "tipo": "multipla_escolha", "area": "xx", "comando": "c",
                  "anulada": false, "status": "disponivel", "credito": "c", "alternativas": %s}]}
                """.formatted(PROVA, ALTERNATIVAS_A))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semChaveDa401() throws Exception {
        mvc.perform(post("/api/v1/ingestao/questoes").contentType(MediaType.APPLICATION_JSON).content(LOTE))
                .andExpect(status().isUnauthorized());
    }

    private Prova prova() {
        return provas.findByBancaAndAnoAndEdicaoAndCaderno(Banca.ENEM, 2024, "D1", "LARANJA").orElseThrow();
    }

    private List<Questao> ordenadas(Prova prova) {
        return questoes.findByProva(prova).stream()
                .sorted(Comparator.comparing(Questao::getNumero)
                        .thenComparing(q -> q.getLingua() == null ? "" : q.getLingua().name(),
                                Comparator.reverseOrder()))
                .toList();
    }

    private ResultActions enviar(String corpo) throws Exception {
        return mvc.perform(post("/api/v1/ingestao/questoes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Api-Key", "chave-de-teste")
                .content(corpo));
    }
}
