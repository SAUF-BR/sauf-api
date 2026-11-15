package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import br.com.sauf_api.questao.Alternativa;
import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import br.com.sauf_api.questao.Lingua;
import br.com.sauf_api.questao.Prova;
import br.com.sauf_api.questao.ProvaRepository;
import br.com.sauf_api.questao.Questao;
import br.com.sauf_api.questao.QuestaoRepository;
import br.com.sauf_api.questao.StatusQuestao;
import br.com.sauf_api.questao.TipoQuestao;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimuladosApiTest {

    /** Relógio que o teste move à vontade. */
    static class RelogioDeTeste extends Clock {
        Instant agora = Instant.parse("2026-11-13T15:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    @TestConfiguration
    static class RelogioConfig {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    // Em todas as questões do teste a alternativa C é a correta.
    static final String CERTA = "C";
    static final String ERRADA = "A";

    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    MockMvc mvc;
    @Autowired ProvaRepository provas;
    @Autowired QuestaoRepository questoes;
    @Autowired RelogioDeTeste relogio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private Prova prova;
    private int proximoNumero;

    @BeforeEach
    void preparar() {
        mvc = Sessao.mvcLogado(contexto, tokens);
        relogio.agora = Instant.parse("2026-11-13T15:00:00Z");
        prova = new Prova(Banca.ENEM, 2024, "D1", "LARANJA");
        prova.atualizar("https://exemplo.test", "ENEM 2024 (INEP)");
        provas.save(prova);
        proximoNumero = 1;
        for (AreaQuestao area : AreaQuestao.values()) {
            semear(area, 12, StatusQuestao.DISPONIVEL, false, null);
        }
    }

    // ---------- opções ----------

    @Test
    void opcoesListaTamanhosBancaEAreasComAsQuestoesSorteaveis() throws Exception {
        semear(AreaQuestao.LC, 3, StatusQuestao.PENDENTE_REVISAO, false, null);
        semear(AreaQuestao.LC, 2, StatusQuestao.DISPONIVEL, true, null);
        semear(AreaQuestao.LC, 2, StatusQuestao.DISPONIVEL, false, Lingua.ESPANHOL);
        semear(AreaQuestao.LC, 1, StatusQuestao.DISPONIVEL, false, Lingua.INGLES);

        mvc.perform(get("/api/v1/simulados/opcoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanhos.length()").value(3))
                .andExpect(jsonPath("$.tamanhos[0].id").value("rapida"))
                .andExpect(jsonPath("$.tamanhos[0].questoes").value(10))
                .andExpect(jsonPath("$.tamanhos[1].id").value("media"))
                .andExpect(jsonPath("$.tamanhos[1].questoes").value(20))
                .andExpect(jsonPath("$.tamanhos[2].id").value("longa"))
                .andExpect(jsonPath("$.tamanhos[2].questoes").value(30))
                .andExpect(jsonPath("$.segundosPorQuestao").value(150))
                .andExpect(jsonPath("$.temposPorQuestao.length()").value(5))
                .andExpect(jsonPath("$.temposPorQuestao[0]").value(90))
                .andExpect(jsonPath("$.temposPorQuestao[4]").value(240))
                .andExpect(jsonPath("$.bancas.length()").value(1))
                .andExpect(jsonPath("$.bancas[0].id").value("enem"))
                .andExpect(jsonPath("$.bancas[0].nome").value("ENEM"))
                .andExpect(jsonPath("$.areas[0].id").value("lc"))
                .andExpect(jsonPath("$.areas[0].nome").value("Linguagens"))
                .andExpect(jsonPath("$.areas[0].questoesDisponiveis").value(13))
                .andExpect(jsonPath("$.areas[1].nome").value("Ciências Humanas"))
                .andExpect(jsonPath("$.areas[1].questoesDisponiveis").value(12))
                .andExpect(jsonPath("$.areas[2].nome").value("Ciências da Natureza"))
                .andExpect(jsonPath("$.areas[3].nome").value("Matemática"));
    }

    // ---------- criar ----------

    @Test
    void criaComAQuantidadeDoTamanhoEDivideEntreAsAreasNaOrdem() throws Exception {
        // 10 questões em 3 áreas: sobra 1, que vai para a primeira (lc 4, ch 3, mt 3)
        String corpo = criar("rapida", "[\"mt\",\"lc\",\"ch\"]", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Simulado ENEM · Linguagens, Ciências Humanas e Matemática"))
                .andExpect(jsonPath("$.banca").value("enem"))
                .andExpect(jsonPath("$.tamanho").value("rapida"))
                .andExpect(jsonPath("$.areas[0]").value("lc"))
                .andExpect(jsonPath("$.areas[2]").value("mt"))
                .andExpect(jsonPath("$.cronometrado").value(true))
                .andExpect(jsonPath("$.segundosPorQuestao").value(150))
                .andExpect(jsonPath("$.status").value("em_andamento"))
                .andExpect(jsonPath("$.iniciadoEm").value("2026-11-13T15:00:00Z"))
                .andExpect(jsonPath("$.finalizadoEm").isEmpty())
                .andExpect(jsonPath("$.totalQuestoes").value(10))
                .andExpect(jsonPath("$.questoes.length()").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> areas = JsonPath.read(corpo, "$.questoes[*].area");
        assertThat(areas).containsExactly("lc", "lc", "lc", "lc", "ch", "ch", "ch", "mt", "mt", "mt");
        List<Integer> ordens = JsonPath.read(corpo, "$.questoes[*].ordem");
        assertThat(ordens).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat((String) JsonPath.read(corpo, "$.questoes[0].situacao")).isEqualTo("pendente");
        assertThat((Integer) JsonPath.read(corpo, "$.questoes[0].ano")).isEqualTo(2024);
        assertThat((List<?>) JsonPath.read(corpo, "$.questoes[0].alternativas")).hasSize(5);
    }

    @Test
    void criaMediaELongaComTodasAsAreas() throws Exception {
        criar("media", "[\"lc\",\"ch\",\"cn\",\"mt\"]", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Simulado ENEM · todas as áreas"))
                .andExpect(jsonPath("$.cronometrado").value(false))
                .andExpect(jsonPath("$.segundosPorQuestao").isEmpty())
                .andExpect(jsonPath("$.totalQuestoes").value(20));

        // 30 em 4 áreas: 8, 8, 7, 7
        String longa = criar("longa", "[\"lc\",\"ch\",\"cn\",\"mt\"]", false)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> areas = JsonPath.read(longa, "$.questoes[*].area");
        assertThat(areas.stream().filter("lc"::equals)).hasSize(8);
        assertThat(areas.stream().filter("ch"::equals)).hasSize(8);
        assertThat(areas.stream().filter("cn"::equals)).hasSize(7);
        assertThat(areas.stream().filter("mt"::equals)).hasSize(7);
    }

    @Test
    void sorteiaSoDisponiveisNaoAnuladasESemEspanhol() throws Exception {
        // as 12 de lc do preparar() são as únicas válidas; entram 10 inválidas
        semear(AreaQuestao.LC, 4, StatusQuestao.PENDENTE_REVISAO, false, null);
        semear(AreaQuestao.LC, 3, StatusQuestao.DISPONIVEL, true, null);
        semear(AreaQuestao.LC, 3, StatusQuestao.DISPONIVEL, false, Lingua.ESPANHOL);

        for (int i = 0; i < 3; i++) {
            String corpo = criar("rapida", "[\"lc\"]", false)
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            List<Integer> numeros = JsonPath.read(corpo, "$.questoes[*].numero");
            // as válidas de lc são as numeradas de 1 a 12
            assertThat(numeros).allMatch(n -> n >= 1 && n <= 12);
            assertThat(new HashSet<>(numeros)).hasSize(10);
        }
    }

    @Test
    void priorizaQuestoesQueOUsuarioNuncaTeve() throws Exception {
        semear(AreaQuestao.LC, 8, StatusQuestao.DISPONIVEL, false, null); // lc com 20 no total
        Set<Integer> primeiro = numeros(criar("rapida", "[\"lc\"]", false));
        Set<Integer> segundo = numeros(criar("rapida", "[\"lc\"]", false));
        assertThat(primeiro).hasSize(10);
        assertThat(segundo).hasSize(10);
        assertThat(primeiro).doesNotContainAnyElementsOf(segundo);

        // o terceiro precisa repetir, mas leva primeiro todas as que sobraram: 0 inéditas
        Set<Integer> terceiro = numeros(criar("rapida", "[\"lc\"]", false));
        assertThat(terceiro).hasSize(10);
    }

    @Test
    void recusaCriacaoInvalida() throws Exception {
        criar("rapida", "[]", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.areas").exists());
        criar("rapida", "[\"xx\"]", true).andExpect(status().isBadRequest());
        criar("gigante", "[\"lc\"]", true).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/simulados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banca\":\"enem\",\"tamanho\":\"rapida\",\"areas\":[\"lc\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.cronometrado").exists());
        mvc.perform(post("/api/v1/simulados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"banca\":\"uem\",\"tamanho\":\"rapida\",\"areas\":[\"lc\"],\"cronometrado\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.banca").exists());
    }

    @Test
    void tempoPorQuestaoEscolhidoNaCriacao() throws Exception {
        criar("rapida", "[\"lc\"]", true, 90)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segundosPorQuestao").value(90));
        criar("rapida", "[\"lc\"]", true, 600).andExpect(status().isCreated());
        criar("rapida", "[\"lc\"]", false, 90)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segundosPorQuestao").isEmpty());

        for (Integer invalido : new Integer[] {null, 30, 135, 630}) {
            criar("rapida", "[\"lc\"]", true, invalido)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fields.segundosPorQuestao").exists());
        }
    }

    @Test
    void recusaQuandoNaoHaQuestoesSuficientes() throws Exception {
        // lc tem 12 e a longa de uma área pede 30
        criar("longa", "[\"lc\"]", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Não há questões suficientes de Linguagens para este simulado"));
    }

    // ---------- detalhe e responder ----------

    @Test
    void detalheNuncaTrazOGabarito() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", true));

        String corpo = mvc.perform(get("/api/v1/simulados/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questoes[0].alternativaCorreta").doesNotExist())
                .andExpect(jsonPath("$.questoes[0].alternativas[0].correta").doesNotExist())
                .andExpect(jsonPath("$.questoes[0].alternativas[0].rotulo").value("A"))
                .andExpect(jsonPath("$.questoes[0].alternativas[0].texto").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(corpo).doesNotContain("correta");

        String resposta = responder(id, 1, CERTA, "respondida", 10)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(resposta).doesNotContain("correta");

        mvc.perform(get("/api/v1/simulados/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void respondeGuardaAlternativaESituacao() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));

        responder(id, 1, "B", "respondida", 30)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordem").value(1))
                .andExpect(jsonPath("$.situacao").value("respondida"))
                .andExpect(jsonPath("$.alternativaMarcada").value("B"))
                .andExpect(jsonPath("$.tempoSegundos").value(30));
        // pode trocar a resposta enquanto o simulado está aberto
        responder(id, 1, "D", "respondida", 45).andExpect(jsonPath("$.alternativaMarcada").value("D"));
        responder(id, 2, null, "em_branco", 12)
                .andExpect(jsonPath("$.situacao").value("em_branco"))
                .andExpect(jsonPath("$.alternativaMarcada").isEmpty());

        mvc.perform(get("/api/v1/simulados/{id}", id))
                .andExpect(jsonPath("$.questoes[0].alternativaMarcada").value("D"))
                .andExpect(jsonPath("$.questoes[1].situacao").value("em_branco"))
                .andExpect(jsonPath("$.questoes[2].situacao").value("pendente"));
    }

    @Test
    void recusaRespostaInvalida() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));

        responder(id, 1, "Z", "respondida", 5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.alternativa").exists());
        responder(id, 1, null, "respondida", 5).andExpect(status().isBadRequest());
        responder(id, 1, null, "tempo_esgotado", 5).andExpect(status().isBadRequest());
        responder(id, 1, "A", "respondida", -1).andExpect(status().isBadRequest());
        responder(id, 99, "A", "respondida", 5).andExpect(status().isNotFound());
    }

    @Test
    void respostaDepoisDoLimiteFicaForaDoTempo() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", true, 90));

        responder(id, 1, CERTA, "respondida", 89)
                .andExpect(jsonPath("$.situacao").value("respondida"))
                .andExpect(jsonPath("$.foraDoTempo").value(false));
        // reenviar a mesma resposta depois do limite não muda nada
        responder(id, 1, CERTA, "respondida", 120).andExpect(jsonPath("$.foraDoTempo").value(false));
        // trocar a resposta depois do limite marca
        responder(id, 1, ERRADA, "respondida", 130).andExpect(jsonPath("$.foraDoTempo").value(true));
        // e a marca fica, mesmo voltando para a resposta de antes
        responder(id, 1, CERTA, "respondida", 140)
                .andExpect(jsonPath("$.alternativaMarcada").value(CERTA))
                .andExpect(jsonPath("$.foraDoTempo").value(true));

        // esgotou sem resposta: continua pendente, aceita resposta e guarda o tempo real
        responder(id, 2, null, "pendente", 400)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("pendente"))
                .andExpect(jsonPath("$.foraDoTempo").value(false))
                .andExpect(jsonPath("$.tempoSegundos").value(400));
        responder(id, 2, CERTA, "respondida", 410)
                .andExpect(jsonPath("$.situacao").value("respondida"))
                .andExpect(jsonPath("$.alternativaMarcada").value(CERTA))
                .andExpect(jsonPath("$.foraDoTempo").value(true))
                .andExpect(jsonPath("$.tempoSegundos").value(410));

        responder(id, 3, null, "em_branco", 200).andExpect(jsonPath("$.foraDoTempo").value(false));

        mvc.perform(get("/api/v1/simulados/{id}", id))
                .andExpect(jsonPath("$.questoes[0].foraDoTempo").value(true))
                .andExpect(jsonPath("$.questoes[3].foraDoTempo").value(false));
    }

    @Test
    void semCronometroNaoHaLimiteEOTempoAcumuladoGuardaOMaior() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));

        responder(id, 1, CERTA, "respondida", 400)
                .andExpect(jsonPath("$.situacao").value("respondida"))
                .andExpect(jsonPath("$.foraDoTempo").value(false));
        responder(id, 1, CERTA, "respondida", 380)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoSegundos").value(400));
        responder(id, 1, CERTA, "respondida", 410).andExpect(jsonPath("$.tempoSegundos").value(410));
    }

    // ---------- finalizar e revisão ----------

    @Test
    void finalizaCorrigeEOSegundoFinalizarDevolveAMesmaRevisao() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        for (int ordem = 1; ordem <= 4; ordem++) {
            responder(id, ordem, CERTA, "respondida", 20);
        }
        responder(id, 5, ERRADA, "respondida", 30);
        responder(id, 6, ERRADA, "respondida", 30);
        responder(id, 7, null, "em_branco", 5);
        // 8 a 10 ficam pendentes

        mvc.perform(get("/api/v1/simulados/{id}/revisao", id))
                .andExpect(status().isConflict());

        relogio.agora = Instant.parse("2026-11-13T16:00:00Z");
        String primeira = mvc.perform(post("/api/v1/simulados/{id}/finalizar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.totalQuestoes").value(10))
                .andExpect(jsonPath("$.acertos").value(4))
                .andExpect(jsonPath("$.erros").value(2))
                .andExpect(jsonPath("$.emBranco").value(4))
                .andExpect(jsonPath("$.respondidasForaDoTempo").value(0))
                .andExpect(jsonPath("$.porcentagemAcerto").value(40.0))
                .andExpect(jsonPath("$.duracaoSegundos").value(4 * 20 + 2 * 30 + 5))
                .andExpect(jsonPath("$.finalizadoEm").value("2026-11-13T16:00:00Z"))
                .andExpect(jsonPath("$.porArea.length()").value(1))
                .andExpect(jsonPath("$.porArea[0].area").value("lc"))
                .andExpect(jsonPath("$.porArea[0].acertos").value(4))
                .andExpect(jsonPath("$.porArea[0].total").value(10))
                .andExpect(jsonPath("$.questoes[0].resultado").value("acerto"))
                .andExpect(jsonPath("$.questoes[0].alternativaCorreta").value(CERTA))
                .andExpect(jsonPath("$.questoes[0].alternativaMarcada").value(CERTA))
                .andExpect(jsonPath("$.questoes[4].resultado").value("erro"))
                .andExpect(jsonPath("$.questoes[4].alternativaMarcada").value(ERRADA))
                .andExpect(jsonPath("$.questoes[6].resultado").value("em_branco"))
                .andExpect(jsonPath("$.questoes[9].resultado").value("em_branco"))
                .andExpect(jsonPath("$.questoes[9].tempoEsgotado").value(false))
                .andExpect(jsonPath("$.questoes[9].foraDoTempo").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        relogio.agora = Instant.parse("2026-11-13T17:00:00Z");
        String segunda = mvc.perform(post("/api/v1/simulados/{id}/finalizar", id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(segunda).isEqualTo(primeira);

        String revisao = mvc.perform(get("/api/v1/simulados/{id}/revisao", id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(revisao).isEqualTo(primeira);

        mvc.perform(get("/api/v1/simulados/{id}", id))
                .andExpect(jsonPath("$.status").value("concluido"))
                .andExpect(jsonPath("$.questoes[9].situacao").value("em_branco"));
        responder(id, 8, CERTA, "respondida", 10).andExpect(status().isConflict());
    }

    @Test
    void respostaForaDoTempoContaNaNotaEEsgotadaSemRespostaFicaEmBranco() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", true));
        responder(id, 1, CERTA, "respondida", 200);
        responder(id, 2, CERTA, "respondida", 20);
        responder(id, 3, ERRADA, "respondida", 160);
        responder(id, 4, null, "pendente", 150);

        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id))
                .andExpect(jsonPath("$.acertos").value(2))
                .andExpect(jsonPath("$.erros").value(1))
                .andExpect(jsonPath("$.emBranco").value(7))
                .andExpect(jsonPath("$.respondidasForaDoTempo").value(2))
                .andExpect(jsonPath("$.porcentagemAcerto").value(20.0))
                .andExpect(jsonPath("$.questoes[0].resultado").value("acerto"))
                .andExpect(jsonPath("$.questoes[0].foraDoTempo").value(true))
                .andExpect(jsonPath("$.questoes[0].tempoEsgotado").value(true))
                .andExpect(jsonPath("$.questoes[1].foraDoTempo").value(false))
                .andExpect(jsonPath("$.questoes[1].tempoEsgotado").value(false))
                .andExpect(jsonPath("$.questoes[2].resultado").value("erro"))
                .andExpect(jsonPath("$.questoes[3].resultado").value("em_branco"))
                .andExpect(jsonPath("$.questoes[3].tempoEsgotado").value(true))
                .andExpect(jsonPath("$.questoes[3].foraDoTempo").value(false))
                .andExpect(jsonPath("$.questoes[3].alternativaMarcada").isEmpty())
                .andExpect(jsonPath("$.questoes[3].alternativaCorreta").value(CERTA));

        mvc.perform(get("/api/v1/simulados"))
                .andExpect(jsonPath("$.content[0].respondidasForaDoTempo").value(2))
                .andExpect(jsonPath("$.content[0].segundosPorQuestao").value(150));
    }

    // ---------- refazer erros ----------

    @Test
    void refazerErrosCriaSimuladoSoComAsErradas() throws Exception {
        UUID id = idDe(criar("media", "[\"lc\",\"mt\"]", true, 120));
        responder(id, 1, CERTA, "respondida", 10);
        responder(id, 2, ERRADA, "respondida", 10);
        responder(id, 11, ERRADA, "respondida", 10); // primeira de mt (10 de lc + 10 de mt)
        responder(id, 12, null, "em_branco", 10);
        String revisao = mvc.perform(post("/api/v1/simulados/{id}/finalizar", id))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> numeros = JsonPath.read(revisao, "$.questoes[?(@.resultado == 'erro')].numero");
        assertThat(numeros).hasSize(2);

        relogio.agora = Instant.parse("2026-11-13T18:00:00Z");
        String novo = mvc.perform(post("/api/v1/simulados/{id}/refazer-erros", id))
                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.tamanho").isEmpty())
                .andExpect(jsonPath("$.cronometrado").value(true))
                .andExpect(jsonPath("$.segundosPorQuestao").value(120))
                .andExpect(jsonPath("$.status").value("em_andamento"))
                .andExpect(jsonPath("$.titulo").value("Refazer erros · Simulado ENEM · Linguagens e Matemática"))
                .andExpect(jsonPath("$.totalQuestoes").value(2))
                .andExpect(jsonPath("$.questoes[0].ordem").value(1))
                .andExpect(jsonPath("$.questoes[1].ordem").value(2))
                .andExpect(jsonPath("$.questoes[0].situacao").value("pendente"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat((String) JsonPath.read(novo, "$.id")).isNotEqualTo(id.toString());
        List<Integer> numerosNovo = JsonPath.read(novo, "$.questoes[*].numero");
        assertThat(numerosNovo).containsExactlyInAnyOrderElementsOf(numeros);
    }

    @Test
    void refazerErrosRecusaSimuladoSemErrosOuEmAndamento() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        mvc.perform(post("/api/v1/simulados/{id}/refazer-erros", id)).andExpect(status().isConflict());

        responder(id, 1, CERTA, "respondida", 10);
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id));
        mvc.perform(post("/api/v1/simulados/{id}/refazer-erros", id)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/simulados/{id}/refazer-erros", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------- lista ----------

    @Test
    void listaComOrdemPorNotaEPaginacao() throws Exception {
        UUID umAcerto = concluido(1);
        UUID seteAcertos = concluido(7);
        UUID cincoAcertos = concluido(5);
        relogio.agora = relogio.agora.plusSeconds(60);
        UUID emAndamento = idDe(criar("rapida", "[\"lc\"]", false));

        mvc.perform(get("/api/v1/simulados").param("ordem", "maior_nota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].id").value(seteAcertos.toString()))
                .andExpect(jsonPath("$.content[0].porcentagemAcerto").value(70.0))
                .andExpect(jsonPath("$.content[1].id").value(cincoAcertos.toString()))
                .andExpect(jsonPath("$.content[2].id").value(umAcerto.toString()))
                .andExpect(jsonPath("$.content[3].id").value(emAndamento.toString()))
                .andExpect(jsonPath("$.content[3].status").value("em_andamento"))
                .andExpect(jsonPath("$.content[3].porcentagemAcerto").isEmpty())
                .andExpect(jsonPath("$.content[3].porArea.length()").value(0));

        mvc.perform(get("/api/v1/simulados").param("ordem", "menor_nota"))
                .andExpect(jsonPath("$.content[0].id").value(umAcerto.toString()))
                .andExpect(jsonPath("$.content[1].id").value(cincoAcertos.toString()))
                .andExpect(jsonPath("$.content[2].id").value(seteAcertos.toString()))
                .andExpect(jsonPath("$.content[3].id").value(emAndamento.toString()));

        mvc.perform(get("/api/v1/simulados").param("ordem", "maior_nota").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(umAcerto.toString()));

        // padrão: do mais recente ao mais antigo, 12 por página
        mvc.perform(get("/api/v1/simulados"))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.content[0].id").value(emAndamento.toString()));
        mvc.perform(get("/api/v1/simulados").param("ordem", "antigo"))
                .andExpect(jsonPath("$.content[0].id").value(umAcerto.toString()));

        mvc.perform(get("/api/v1/simulados").param("status", "concluido"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/simulados").param("status", "em_andamento"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(emAndamento.toString()));
        mvc.perform(get("/api/v1/simulados").param("ordem", "aleatoria")).andExpect(status().isBadRequest());
    }

    @Test
    void itemDaListaTrazContagensEPorArea() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\",\"mt\"]", false));
        for (int ordem = 1; ordem <= 5; ordem++) {
            responder(id, ordem, CERTA, "respondida", 10);
        }
        responder(id, 6, ERRADA, "respondida", 10);
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id));

        mvc.perform(get("/api/v1/simulados"))
                .andExpect(jsonPath("$.content[0].titulo").value("Simulado ENEM · Linguagens e Matemática"))
                .andExpect(jsonPath("$.content[0].tamanho").value("rapida"))
                .andExpect(jsonPath("$.content[0].cronometrado").value(false))
                .andExpect(jsonPath("$.content[0].segundosPorQuestao").isEmpty())
                .andExpect(jsonPath("$.content[0].totalQuestoes").value(10))
                .andExpect(jsonPath("$.content[0].respondidas").value(6))
                .andExpect(jsonPath("$.content[0].respondidasForaDoTempo").value(0))
                .andExpect(jsonPath("$.content[0].acertos").value(5))
                .andExpect(jsonPath("$.content[0].erros").value(1))
                .andExpect(jsonPath("$.content[0].emBranco").value(4))
                .andExpect(jsonPath("$.content[0].porcentagemAcerto").value(50.0))
                .andExpect(jsonPath("$.content[0].duracaoSegundos").value(60))
                .andExpect(jsonPath("$.content[0].areas[1]").value("mt"))
                .andExpect(jsonPath("$.content[0].porArea[0].area").value("lc"))
                .andExpect(jsonPath("$.content[0].porArea[0].acertos").value(5))
                .andExpect(jsonPath("$.content[0].porArea[0].total").value(5))
                .andExpect(jsonPath("$.content[0].porArea[1].area").value("mt"))
                .andExpect(jsonPath("$.content[0].porArea[1].acertos").value(0));
    }

    // ---------- resumo ----------

    @Test
    void resumoSemSimuladosConcluidos() throws Exception {
        mvc.perform(get("/api/v1/simulados/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simuladosFeitos").value(0))
                .andExpect(jsonPath("$.questoesRespondidas").value(0))
                .andExpect(jsonPath("$.aproveitamentoMedio").isEmpty())
                .andExpect(jsonPath("$.melhorArea").isEmpty());
    }

    @Test
    void resumoCalculaAproveitamentoEMelhorArea() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\",\"mt\"]", false));
        for (int ordem = 1; ordem <= 5; ordem++) {
            responder(id, ordem, CERTA, "respondida", 10); // lc: 5 de 5
        }
        responder(id, 6, CERTA, "respondida", 10); // mt: 2 de 5
        responder(id, 7, CERTA, "respondida", 10);
        responder(id, 8, ERRADA, "respondida", 10);
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id));

        UUID aberto = idDe(criar("rapida", "[\"ch\"]", false));
        responder(aberto, 1, CERTA, "respondida", 10);
        responder(aberto, 2, CERTA, "respondida", 10);
        responder(aberto, 3, null, "em_branco", 10);

        mvc.perform(get("/api/v1/simulados/resumo"))
                .andExpect(jsonPath("$.simuladosFeitos").value(1))
                .andExpect(jsonPath("$.questoesRespondidas").value(10))
                .andExpect(jsonPath("$.aproveitamentoMedio").value(70.0))
                .andExpect(jsonPath("$.melhorArea.area").value("lc"))
                .andExpect(jsonPath("$.melhorArea.porcentagem").value(100.0));
    }

    // ---------- cronômetro ----------

    @Test
    void cronometroContaOsDiasNoFusoDeSaoPauloEMontaOsDados() throws Exception {
        relogio.agora = Instant.parse("2026-11-12T12:00:00Z");
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));

        // 02:59Z ainda é 23:59 do dia 12 em São Paulo; 03:00Z já é o dia 13
        relogio.agora = Instant.parse("2026-11-13T02:59:00Z");
        responder(id, 1, CERTA, "respondida", 30);
        relogio.agora = Instant.parse("2026-11-13T03:00:00Z");
        responder(id, 2, CERTA, "respondida", 45);
        // 02:30Z do dia 14 (UTC) ainda é 23:30 do dia 13 em São Paulo
        relogio.agora = Instant.parse("2026-11-14T02:30:00Z");
        responder(id, 3, CERTA, "respondida", 60);
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id));

        String corpo = mvc.perform(get("/api/v1/simulados/cronometro").param("periodo", "semana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoTotal.atual").value(135))
                .andExpect(jsonPath("$.tempoTotal.anterior").value(0))
                .andExpect(jsonPath("$.tempoMedioQuestao.atual").value(45))
                .andExpect(jsonPath("$.tempoMedioQuestao.anterior").value(0))
                .andExpect(jsonPath("$.diasAtivos.atual").value(2))
                .andExpect(jsonPath("$.diasAtivos.anterior").value(0))
                .andExpect(jsonPath("$.ofensiva.atual").value(2))
                .andExpect(jsonPath("$.ofensiva.recorde").value(2))
                .andExpect(jsonPath("$.dias.length()").value(365))
                .andExpect(jsonPath("$.dias[364].data").value("2026-11-13"))
                .andExpect(jsonPath("$.dias[364].segundos").value(105))
                .andExpect(jsonPath("$.dias[363].data").value("2026-11-12"))
                .andExpect(jsonPath("$.dias[363].segundos").value(30))
                .andExpect(jsonPath("$.dias[0].data").value("2025-11-14"))
                .andExpect(jsonPath("$.dias[100].segundos").value(0))
                .andExpect(jsonPath("$.porConfiguracao.length()").value(1))
                .andExpect(jsonPath("$.porConfiguracao[0].tamanho").value("rapida"))
                .andExpect(jsonPath("$.porConfiguracao[0].questoes").value(10))
                .andExpect(jsonPath("$.porConfiguracao[0].simulados").value(1))
                .andExpect(jsonPath("$.porConfiguracao[0].mediaSegundos").value(135))
                .andExpect(jsonPath("$.porConfiguracao[0].totalSegundos").value(135))
                .andExpect(jsonPath("$.porConfiguracao[0].segundosPorQuestao").value(14))
                .andExpect(jsonPath("$.porConfiguracao[0].observacao").isEmpty())
                .andExpect(jsonPath("$.horasPorSemana.length()").value(4))
                .andExpect(jsonPath("$.horasPorSemana[0].rotulo").value("S1"))
                .andExpect(jsonPath("$.horasPorSemana[0].segundos").value(0))
                .andExpect(jsonPath("$.horasPorSemana[3].rotulo").value("S4"))
                .andExpect(jsonPath("$.horasPorSemana[3].segundos").value(135))
                .andExpect(jsonPath("$.ritmoPorCategoria.length()").value(1))
                .andExpect(jsonPath("$.ritmoPorCategoria[0].categoria").value("lc"))
                .andExpect(jsonPath("$.ritmoPorCategoria[0].segundosPorQuestao").value(45))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat((String) JsonPath.read(corpo, "$.dias[364].data")).isEqualTo("2026-11-13");

        mvc.perform(get("/api/v1/simulados/cronometro").param("periodo", "mes"))
                .andExpect(jsonPath("$.tempoTotal.atual").value(135))
                .andExpect(jsonPath("$.tempoTotal.anterior").value(0));
        // o padrão é a semana
        mvc.perform(get("/api/v1/simulados/cronometro"))
                .andExpect(jsonPath("$.tempoTotal.atual").value(135));
        mvc.perform(get("/api/v1/simulados/cronometro").param("periodo", "decada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cronometroAnoNaoTemJanelaAnterior() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        responder(id, 1, CERTA, "respondida", 90);

        mvc.perform(get("/api/v1/simulados/cronometro").param("periodo", "ano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoTotal.atual").value(90))
                .andExpect(jsonPath("$.tempoTotal.anterior").isEmpty())
                .andExpect(jsonPath("$.tempoMedioQuestao.anterior").isEmpty())
                .andExpect(jsonPath("$.diasAtivos.atual").value(1))
                .andExpect(jsonPath("$.diasAtivos.anterior").isEmpty())
                .andExpect(jsonPath("$.dias.length()").value(365));
    }

    @Test
    void cronometroSemEstudoVemZeradoComOsDias() throws Exception {
        criar("rapida", "[\"lc\"]", false); // só abrir o simulado não é estudo

        mvc.perform(get("/api/v1/simulados/cronometro").param("periodo", "mes"))
                .andExpect(jsonPath("$.tempoTotal.atual").value(0))
                .andExpect(jsonPath("$.tempoMedioQuestao.atual").value(0))
                .andExpect(jsonPath("$.diasAtivos.atual").value(0))
                .andExpect(jsonPath("$.ofensiva.atual").value(0))
                .andExpect(jsonPath("$.ofensiva.recorde").value(0))
                .andExpect(jsonPath("$.dias.length()").value(365))
                .andExpect(jsonPath("$.porConfiguracao.length()").value(0))
                .andExpect(jsonPath("$.ritmoPorCategoria.length()").value(0));
    }

    @Test
    void ofensivaAindaVale_seHojeNaoEstudouMasOntemSim() throws Exception {
        relogio.agora = Instant.parse("2026-11-11T15:00:00Z");
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        responder(id, 1, CERTA, "respondida", 10); // dia 11
        relogio.agora = Instant.parse("2026-11-12T15:00:00Z");
        responder(id, 2, CERTA, "respondida", 10); // dia 12
        relogio.agora = Instant.parse("2026-11-13T15:00:00Z");
        responder(id, 3, CERTA, "respondida", 10); // dia 13

        relogio.agora = Instant.parse("2026-11-14T15:00:00Z"); // dia 14, ainda sem estudo
        mvc.perform(get("/api/v1/simulados/cronometro"))
                .andExpect(jsonPath("$.ofensiva.atual").value(3))
                .andExpect(jsonPath("$.ofensiva.recorde").value(3));

        relogio.agora = Instant.parse("2026-11-15T15:00:00Z"); // quebrou
        mvc.perform(get("/api/v1/simulados/cronometro"))
                .andExpect(jsonPath("$.ofensiva.atual").value(0))
                .andExpect(jsonPath("$.ofensiva.recorde").value(3));
    }

    // ---------- acompanhamento ----------

    @Test
    void acompanhamentoSemSimuladosVemVazio() throws Exception {
        mvc.perform(get("/api/v1/simulados/acompanhamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("mes"))
                .andExpect(jsonPath("$.janela.inicio").value("2026-10-15"))
                .andExpect(jsonPath("$.janela.fim").value("2026-11-13"))
                .andExpect(jsonPath("$.resumo.aproveitamento.atual").isEmpty())
                .andExpect(jsonPath("$.resumo.aproveitamento.anterior").isEmpty())
                .andExpect(jsonPath("$.resumo.questoesRespondidas.atual").value(0))
                .andExpect(jsonPath("$.resumo.simuladosConcluidos.atual").value(0))
                .andExpect(jsonPath("$.evolucao.mediaPlataforma").isEmpty())
                .andExpect(jsonPath("$.evolucao.simulados.length()").value(0))
                .andExpect(jsonPath("$.distribuicao.atual.acertos").value(0))
                .andExpect(jsonPath("$.areas.length()").value(4))
                .andExpect(jsonPath("$.areas[0].aproveitamento.atual").isEmpty())
                .andExpect(jsonPath("$.focos.length()").value(0))
                .andExpect(jsonPath("$.constancia.diasAtivos").value(0))
                .andExpect(jsonPath("$.constancia.diasNaJanela").value(30))
                .andExpect(jsonPath("$.constancia.ofensiva").value(0))
                .andExpect(jsonPath("$.constancia.semanas.length()").value(4))
                .andExpect(jsonPath("$.constancia.semanas[3].inicio").value("2026-11-07"));
    }

    @Test
    void acompanhamentoComparaComAJanelaAnterior() throws Exception {
        relogio.agora = Instant.parse("2026-11-03T15:00:00Z"); // semana anterior (31/10 a 06/11)
        concluido(5);
        relogio.agora = Instant.parse("2026-11-13T15:00:00Z"); // semana atual (07/11 a 13/11)
        concluido(7);

        mvc.perform(get("/api/v1/simulados/acompanhamento").param("periodo", "semana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.janela.inicio").value("2026-11-07"))
                .andExpect(jsonPath("$.resumo.aproveitamento.atual").value(70.0))
                .andExpect(jsonPath("$.resumo.aproveitamento.anterior").value(50.0))
                .andExpect(jsonPath("$.resumo.questoesRespondidas.atual").value(10))
                .andExpect(jsonPath("$.resumo.questoesRespondidas.anterior").value(10))
                .andExpect(jsonPath("$.resumo.simuladosConcluidos.atual").value(1))
                .andExpect(jsonPath("$.resumo.simuladosConcluidos.anterior").value(1))
                .andExpect(jsonPath("$.resumo.acertos.atual").value(7))
                .andExpect(jsonPath("$.resumo.acertos.anterior").value(5))
                .andExpect(jsonPath("$.evolucao.mediaPlataforma").isEmpty())
                .andExpect(jsonPath("$.evolucao.simulados.length()").value(1))
                .andExpect(jsonPath("$.evolucao.simulados[0].aproveitamento").value(70.0))
                .andExpect(jsonPath("$.distribuicao.atual.acertos").value(7))
                .andExpect(jsonPath("$.distribuicao.atual.erros").value(3))
                .andExpect(jsonPath("$.distribuicao.atual.emBranco").value(0))
                .andExpect(jsonPath("$.distribuicao.anterior.erros").value(5))
                .andExpect(jsonPath("$.areas[0].area").value("lc"))
                .andExpect(jsonPath("$.areas[0].aproveitamento.atual").value(70.0))
                .andExpect(jsonPath("$.areas[0].aproveitamento.anterior").value(50.0))
                .andExpect(jsonPath("$.areas[0].respondidas").value(10))
                .andExpect(jsonPath("$.areas[3].area").value("mt"))
                .andExpect(jsonPath("$.areas[3].aproveitamento.atual").isEmpty())
                // sem habilidade nas questões, o foco é a área
                .andExpect(jsonPath("$.focos.length()").value(1))
                .andExpect(jsonPath("$.focos[0].area").value("lc"))
                .andExpect(jsonPath("$.focos[0].habilidade").isEmpty())
                .andExpect(jsonPath("$.focos[0].erros").value(3))
                .andExpect(jsonPath("$.constancia.diasAtivos").value(1))
                .andExpect(jsonPath("$.constancia.diasNaJanela").value(7))
                .andExpect(jsonPath("$.constancia.ofensiva").value(1))
                .andExpect(jsonPath("$.constancia.semanas.length()").value(7))
                .andExpect(jsonPath("$.constancia.semanas[6].inicio").value("2026-11-13"))
                .andExpect(jsonPath("$.constancia.semanas[6].diasAtivos").value(1));

        mvc.perform(get("/api/v1/simulados/acompanhamento").param("periodo", "ano"))
                .andExpect(jsonPath("$.resumo.simuladosConcluidos.atual").value(2))
                .andExpect(jsonPath("$.evolucao.simulados.length()").value(2))
                .andExpect(jsonPath("$.evolucao.simulados[0].aproveitamento").value(50.0))
                .andExpect(jsonPath("$.constancia.diasAtivos").value(2))
                .andExpect(jsonPath("$.constancia.semanas.length()").value(12))
                .andExpect(jsonPath("$.constancia.semanas[11].inicio").value("2026-11-01"))
                .andExpect(jsonPath("$.constancia.semanas[11].diasAtivos").value(2));

        mvc.perform(get("/api/v1/simulados/acompanhamento").param("vestibular", "uem"))
                .andExpect(jsonPath("$.resumo.aproveitamento.atual").isEmpty());
        mvc.perform(get("/api/v1/simulados/acompanhamento").param("periodo", "decada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acompanhamentoSoComQuestoesEmBranco() throws Exception {
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/simulados/acompanhamento"))
                .andExpect(jsonPath("$.resumo.aproveitamento.atual").value(0.0))
                .andExpect(jsonPath("$.resumo.questoesRespondidas.atual").value(0))
                .andExpect(jsonPath("$.resumo.simuladosConcluidos.atual").value(1))
                .andExpect(jsonPath("$.distribuicao.atual.emBranco").value(10))
                .andExpect(jsonPath("$.focos.length()").value(0))
                .andExpect(jsonPath("$.constancia.diasAtivos").value(0));
    }

    @Test
    void focosVemDasHabilidadesComMaisErrosEDepoisDasAreas() throws Exception {
        em.createQuery("update Questao q set q.habilidade = 17 where q.area = :area")
                .setParameter("area", AreaQuestao.CN)
                .executeUpdate();
        UUID id = idDe(criar("rapida", "[\"cn\"]", false));
        for (int ordem = 1; ordem <= 10; ordem++) {
            responder(id, ordem, ordem <= 6 ? CERTA : ERRADA, "respondida", 10);
        }
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id)).andExpect(status().isOk());
        concluido(8); // lc sem habilidade, 2 erros

        mvc.perform(get("/api/v1/simulados/acompanhamento"))
                .andExpect(jsonPath("$.focos.length()").value(2))
                .andExpect(jsonPath("$.focos[0].ordem").value(1))
                .andExpect(jsonPath("$.focos[0].area").value("cn"))
                .andExpect(jsonPath("$.focos[0].habilidade").value("H17"))
                .andExpect(jsonPath("$.focos[0].descricao").value(Matchers.startsWith(
                        "Relacionar informações apresentadas em diferentes formas de linguagem")))
                .andExpect(jsonPath("$.focos[0].erros").value(4))
                .andExpect(jsonPath("$.focos[0].respondidas").value(10))
                .andExpect(jsonPath("$.focos[1].area").value("lc"))
                .andExpect(jsonPath("$.focos[1].habilidade").isEmpty())
                .andExpect(jsonPath("$.focos[1].erros").value(2));
    }

    @Test
    void mediaDaPlataformaSoComCincoAlunos() throws Exception {
        concluido(7);
        simuladoDeOutroAluno(5);
        simuladoDeOutroAluno(6);
        simuladoDeOutroAluno(8);

        mvc.perform(get("/api/v1/simulados/acompanhamento"))
                .andExpect(jsonPath("$.evolucao.mediaPlataforma").isEmpty());

        simuladoDeOutroAluno(9);
        mvc.perform(get("/api/v1/simulados/acompanhamento"))
                .andExpect(jsonPath("$.evolucao.mediaPlataforma").value(70.0));
    }

    // ---------- ranking ----------

    // Alunos de src/main/resources/mock/perfis-publicos.json
    static UUID aluno(int n) {
        return UUID.fromString("5eed0000-0000-4000-8000-%012d".formatted(n));
    }

    @Test
    void rankingSemTempoNaJanela() throws Exception {
        mvc.perform(get("/api/v1/simulados/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("mes"))
                .andExpect(jsonPath("$.janela.inicio").value("2026-11-01"))
                .andExpect(jsonPath("$.janela.fim").value("2026-11-30"))
                .andExpect(jsonPath("$.totalParticipantes").value(0))
                .andExpect(jsonPath("$.podio.length()").value(0))
                .andExpect(jsonPath("$.colocacao").isEmpty())
                .andExpect(jsonPath("$.percentil").isEmpty())
                .andExpect(jsonPath("$.estatisticas.segundos").value(0))
                .andExpect(jsonPath("$.estatisticas.simulados").value(0));

        // 13/11/2026 é sexta: a semana do calendário vai de segunda (09) a domingo (15)
        mvc.perform(get("/api/v1/simulados/ranking").param("periodo", "semana"))
                .andExpect(jsonPath("$.janela.inicio").value("2026-11-09"))
                .andExpect(jsonPath("$.janela.fim").value("2026-11-15"));
        mvc.perform(get("/api/v1/simulados/ranking").param("periodo", "decada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rankingLimitaOTempoDesempataEFiltraOPodio() throws Exception {
        Instant hoje = relogio.agora;
        simuladoComTempo(aluno(1), 200, hoje); // 10 × 150 (limite) = 1500, 1º
        simuladoComTempo(aluno(2), 140, hoje); // 1400, 2º, não quer aparecer
        simuladoComTempo(aluno(3), 60, hoje); // 1200 em 2 simulados, 3º
        simuladoComTempo(aluno(3), 60, hoje);
        simuladoComTempo(aluno(5), 120, hoje); // 1200 em 1 simulado, 4º
        simuladoComTempo(aluno(4), 110, hoje); // 1100, 5º, menor de idade
        simuladoComTempo(aluno(6), 90, Instant.parse("2026-10-31T12:00:00Z")); // mês anterior, fora

        UUID id = idDe(criar("rapida", "[\"lc\"]", true));
        for (int ordem = 1; ordem <= 10; ordem++) {
            responder(id, ordem, CERTA, "respondida", 50);
        }
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id)).andExpect(status().isOk());
        // Em outubro: o aluno 6 com 900 s e o aluno de teste com 300 s
        simuladoComTempo(Sessao.USUARIO_TESTE, 30, Instant.parse("2026-10-20T12:00:00Z"));

        mvc.perform(get("/api/v1/simulados/ranking"))
                .andExpect(jsonPath("$.totalParticipantes").value(6))
                .andExpect(jsonPath("$.podio.length()").value(2))
                .andExpect(jsonPath("$.podio[0].posicao").value(1))
                .andExpect(jsonPath("$.podio[0].nome").value("Larissa C."))
                .andExpect(jsonPath("$.podio[0].cidade").value("Maringá"))
                .andExpect(jsonPath("$.podio[0].uf").value("PR"))
                .andExpect(jsonPath("$.podio[0].segundos").value(1500))
                .andExpect(jsonPath("$.podio[0].simulados").value(1))
                .andExpect(jsonPath("$.podio[1].posicao").value(3))
                .andExpect(jsonPath("$.podio[1].nome").value("Camila R."))
                .andExpect(jsonPath("$.podio[1].simulados").value(2))
                .andExpect(jsonPath("$.colocacao.posicao").value(6))
                .andExpect(jsonPath("$.colocacao.anterior").value(2))
                .andExpect(jsonPath("$.percentil").isEmpty())
                .andExpect(jsonPath("$.estatisticas.segundos").value(500))
                .andExpect(jsonPath("$.estatisticas.simulados").value(1))
                .andExpect(jsonPath("$.estatisticas.ofensiva").value(1));
    }

    @Test
    void percentilSoComVinteParticipantesNaRegiao() throws Exception {
        Instant hoje = relogio.agora;
        // 19 alunos do PR (tempos de 100 a 1900 por simulado) e um de SP na frente de todos
        int[] doParana = {1, 2, 3, 4, 5, 7, 9, 10, 12, 14, 15, 16, 18, 19, 20, 22, 23, 24, 25};
        for (int i = 0; i < doParana.length - 1; i++) {
            simuladoComTempo(aluno(doParana[i]), 10 * (i + 1), hoje);
        }
        simuladoComTempo(aluno(6), 150, hoje);
        simuladoComTempo(Sessao.USUARIO_TESTE, 105, hoje); // 1050 s, do PR

        // 18 do PR + o aluno de teste = 19: ainda sem percentil
        mvc.perform(get("/api/v1/simulados/ranking"))
                .andExpect(jsonPath("$.percentil").isEmpty());

        simuladoComTempo(aluno(doParana[doParana.length - 1]), 190, hoje);
        // 20 no PR; 9 com mais tempo (1100 a 1900): 10º de 20, entre os primeiros 50%
        mvc.perform(get("/api/v1/simulados/ranking"))
                .andExpect(jsonPath("$.totalParticipantes").value(21))
                .andExpect(jsonPath("$.colocacao.posicao").value(11))
                .andExpect(jsonPath("$.percentil.percentual").value(50))
                .andExpect(jsonPath("$.percentil.regiao").value("PR"));
    }

    // ---------- apoio ----------

    // Simulado rápido concluído (cronometrado com limite de 150 s), com o mesmo tempo nas 10 questões.
    private void simuladoComTempo(UUID usuario, int segundosPorQuestao, Instant finalizadoEm) {
        UUID id = UUID.randomUUID();
        Timestamp quando = Timestamp.from(finalizadoEm);
        jdbc.update("""
                INSERT INTO simulado (id, usuario_id, banca, tamanho, cronometrado, segundos_por_questao, status,
                                      iniciado_em, finalizado_em, acertos, erros, em_branco)
                VALUES (?, ?, 'ENEM', 'RAPIDA', TRUE, 150, 'CONCLUIDO', ?, ?, 5, 5, 0)
                """, id, usuario, quando, quando);
        List<Questao> todas = questoes.findAll();
        for (int ordem = 1; ordem <= 10; ordem++) {
            jdbc.update("""
                    INSERT INTO simulado_questao (simulado_id, ordem, questao_id, situacao, alternativa_marcada,
                                                  correta, tempo_segundos, atualizada_em, fora_do_tempo)
                    VALUES (?, ?, ?, 'RESPONDIDA', 'C', TRUE, ?, ?, FALSE)
                    """, id, ordem, todas.get(ordem - 1).getId(), segundosPorQuestao, quando);
        }
    }

    // Simulado rápido de lc já concluído por outro aluno, direto no banco, finalizado agora.
    private UUID simuladoDeOutroAluno(int acertos) {
        UUID id = UUID.randomUUID();
        Timestamp agora = Timestamp.from(relogio.agora);
        jdbc.update("""
                INSERT INTO simulado (id, usuario_id, banca, tamanho, cronometrado, status, iniciado_em,
                                      finalizado_em, acertos, erros, em_branco)
                VALUES (?, ?, 'ENEM', 'RAPIDA', FALSE, 'CONCLUIDO', ?, ?, ?, ?, 0)
                """, id, UUID.randomUUID(), agora, agora, acertos, 10 - acertos);
        return id;
    }

    // Cronometrado sem tempo informado usa o padrão de 150 s.
    private ResultActions criar(String tamanho, String areas, boolean cronometrado) throws Exception {
        return criar(tamanho, areas, cronometrado, cronometrado ? 150 : null);
    }

    private ResultActions criar(String tamanho, String areas, boolean cronometrado, Integer segundosPorQuestao)
            throws Exception {
        return mvc.perform(post("/api/v1/simulados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"banca": "enem", "tamanho": "%s", "areas": %s, "cronometrado": %s, "segundosPorQuestao": %s}
                        """.formatted(tamanho, areas, cronometrado, segundosPorQuestao)));
    }

    private ResultActions responder(UUID id, int ordem, String alternativa, String situacao, int tempo)
            throws Exception {
        String marcada = alternativa == null ? "null" : "\"" + alternativa + "\"";
        return mvc.perform(put("/api/v1/simulados/{id}/questoes/{ordem}", id, ordem)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"alternativa": %s, "situacao": "%s", "tempoSegundos": %d}
                        """.formatted(marcada, situacao, tempo)));
    }

    private UUID idDe(ResultActions criacao) throws Exception {
        String corpo = criacao.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(corpo, "$.id"));
    }

    private Set<Integer> numeros(ResultActions criacao) throws Exception {
        String corpo = criacao.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        List<Integer> numeros = JsonPath.read(corpo, "$.questoes[*].numero");
        return new HashSet<>(numeros);
    }

    // Simulado rápido de lc finalizado com o número de acertos pedido; cada um começa 1 minuto depois do anterior.
    private UUID concluido(int acertos) throws Exception {
        relogio.agora = relogio.agora.plusSeconds(60);
        UUID id = idDe(criar("rapida", "[\"lc\"]", false));
        for (int ordem = 1; ordem <= acertos; ordem++) {
            responder(id, ordem, CERTA, "respondida", 10);
        }
        for (int ordem = acertos + 1; ordem <= 10; ordem++) {
            responder(id, ordem, ERRADA, "respondida", 10);
        }
        mvc.perform(post("/api/v1/simulados/{id}/finalizar", id)).andExpect(status().isOk());
        return id;
    }

    // Questões de múltipla escolha com a C certa; os números seguem em sequência na prova.
    private void semear(AreaQuestao area, int quantidade, StatusQuestao statusQuestao, boolean anulada, Lingua lingua) {
        for (int i = 0; i < quantidade; i++) {
            Questao questao = new Questao(prova, proximoNumero++, lingua);
            questao.atualizar(new Questao.Conteudo(
                    null,
                    TipoQuestao.MULTIPLA_ESCOLHA,
                    area,
                    null,
                    null,
                    null,
                    "Comando da questão " + (proximoNumero - 1),
                    null,
                    null,
                    anulada,
                    statusQuestao,
                    statusQuestao == StatusQuestao.PENDENTE_REVISAO ? "tabela com sobras" : null,
                    null,
                    "ENEM 2024 (INEP)",
                    List.of(
                            new Alternativa("A", null, "alternativa a", false),
                            new Alternativa("B", null, "alternativa b", false),
                            new Alternativa("C", null, "alternativa c", !anulada),
                            new Alternativa("D", null, "alternativa d", false),
                            new Alternativa("E", null, "alternativa e", false))));
            questoes.save(questao);
        }
    }
}
