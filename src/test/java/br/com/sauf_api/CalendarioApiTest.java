package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.instituicao.TipoInstituicao;
import br.com.sauf_api.usuario.Papel;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CalendarioApiTest {

    // 02h UTC de 20/11 = 23h de 19/11 em Brasília: para o calendário, "hoje" é 19/11/2026
    @TestConfiguration
    static class RelogioConfig {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(Instant.parse("2026-11-20T02:00:00Z"), ZoneOffset.UTC);
        }
    }

    static final String ROTA = "/api/v1/admin/calendario/eventos/";

    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired UsuarioRepository usuarios;
    MockMvc mvc;
    Instituicao uem;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        uem = new Instituicao(57, "Universidade Estadual de Maringá", TipoInstituicao.PUBLICA, "PR", "Maringá");
        uem.setSigla("UEM");
        uem = instituicoes.save(uem);
    }

    static String evento(String titulo, String tipo, String inicio, String fim, Integer codigoEmec) {
        return """
                {"titulo": "%s", "tipo": "%s", "inicio": "%s", "fim": %s, "codigoEmecInstituicao": %s,
                 "descricao": "  ", "fonteUrl": "https://exemplo.gov.br/edital", "anoReferencia": 2027}
                """.formatted(titulo, tipo, inicio, fim == null ? "null" : "\"" + fim + "\"", codigoEmec);
    }

    ResultActions enviar(String chave, String corpo) throws Exception {
        return mvc.perform(put(ROTA + chave)
                .header("X-Api-Key", "chave-de-teste")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    void popular() throws Exception {
        enviar("uem-verao-2026-prova", evento("Prova do Vestibular de Verão UEM", "prova", "2026-12-06", null, 57))
                .andExpect(status().isOk());
        enviar("enem-2026-prova-2", evento("ENEM 2026: 2º dia", "prova", "2026-11-15", null, null))
                .andExpect(status().isOk());
        enviar("ufsm-2027-inscricao", evento("Inscrições UFSM 2027", "inscricao", "2026-10-02", "2026-11-19", null))
                .andExpect(status().isOk());
        enviar("enem-2026-resultado", evento("Resultado do ENEM 2026", "resultado", "2027-01-18", null, null))
                .andExpect(status().isOk());
    }

    @Test
    void putCriaAtualizaEReenviarIgualNaoMuda() throws Exception {
        String corpo = evento("Prova UEM", "prova", "2026-12-06", null, 57);
        enviar("uem-prova", corpo).andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(1))
                .andExpect(jsonPath("$.atualizados").value(0));
        enviar("uem-prova", corpo).andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(0))
                .andExpect(jsonPath("$.atualizados").value(0));
        enviar("uem-prova", evento("Prova UEM", "prova", "2026-12-07", null, 57)).andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizados").value(1));

        mvc.perform(get("/api/v1/calendario/eventos").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].chave").value("uem-prova"))
                .andExpect(jsonPath("$[0].tipo").value("prova"))
                .andExpect(jsonPath("$[0].data").value("2026-12-07"))
                .andExpect(jsonPath("$[0].fim").value(nullValue()))
                .andExpect(jsonPath("$[0].descricao").value(nullValue()))
                .andExpect(jsonPath("$[0].instituicao.sigla").value("UEM"));
    }

    @Test
    void eventosDoAnoIncluemPeriodosQueCruzamOAno() throws Exception {
        popular();
        enviar("virada", evento("Período na virada", "outro", "2026-12-20", "2027-01-10", null)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/calendario/eventos").param("ano", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].chave", contains("virada", "enem-2026-resultado")))
                .andExpect(jsonPath("$[0].inicio").value("2026-12-20"))
                .andExpect(jsonPath("$[0].fim").value("2027-01-10"));
        mvc.perform(get("/api/v1/calendario/eventos").param("ano", "1999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.ano").exists());
    }

    @Test
    void proximosPrazosUsamODiaDeBrasiliaEOFimDoPeriodo() throws Exception {
        popular();

        // O ENEM de 15/11 já passou; a inscrição da UFSM termina "hoje" (19/11 em Brasília)
        mvc.perform(get("/api/v1/calendario/proximos-prazos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].titulo").value("Inscrições UFSM 2027"))
                .andExpect(jsonPath("$[0].data").value("2026-11-19"))
                .andExpect(jsonPath("$[0].descricao").value("Encerra hoje"))
                .andExpect(jsonPath("$[1].data").value("2026-12-06"))
                .andExpect(jsonPath("$[1].descricao").value("Em 17 dias"))
                .andExpect(jsonPath("$[2].titulo").value("Resultado do ENEM 2026"));
        mvc.perform(get("/api/v1/calendario/proximos-prazos").param("limite", "1"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/calendario/proximos-prazos").param("limite", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalheDaUniversidadeMostraOProximoPrazoDela() throws Exception {
        mvc.perform(get("/api/v1/universidades/" + uem.getId()))
                .andExpect(jsonPath("$.proximoPrazo").value(nullValue()));
        popular();

        mvc.perform(get("/api/v1/universidades/" + uem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoPrazo.titulo").value("Prova do Vestibular de Verão UEM"))
                .andExpect(jsonPath("$.proximoPrazo.data").value("2026-12-06"))
                .andExpect(jsonPath("$.proximoPrazo.editalUrl").value("https://exemplo.gov.br/edital"));
    }

    @Test
    void validacoes() throws Exception {
        enviar("Chave Ruim", evento("X", "prova", "2026-12-06", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.chave").exists());
        enviar("periodo", evento("X", "inscricao", "2026-12-06", "2026-12-01", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fim").exists());
        enviar("tipo", evento("X", "palestra", "2026-12-06", null, null))
                .andExpect(status().isBadRequest());
        enviar("sem-titulo", evento("", "prova", "2026-12-06", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.titulo").exists());
        enviar("ies", evento("X", "prova", "2026-12-06", null, 999))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void deleteRemoveE404SeNaoExiste() throws Exception {
        popular();
        mvc.perform(delete(ROTA + "enem-2026-prova-2").header("X-Api-Key", "chave-de-teste"))
                .andExpect(status().isNoContent());
        mvc.perform(delete(ROTA + "enem-2026-prova-2").header("X-Api-Key", "chave-de-teste"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/calendario/eventos").param("ano", "2026"))
                .andExpect(jsonPath("$[*].chave", contains("ufsm-2027-inscricao", "uem-verao-2026-prova")));
    }

    @Test
    void adminSemChaveNemSessao401ComChaveErrada401() throws Exception {
        String corpo = evento("X", "prova", "2026-12-06", null, null);
        mvc.perform(put(ROTA + "x").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA + "x").header("X-Api-Key", "errada").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(ROTA + "x")).andExpect(status().isUnauthorized());
    }

    @Test
    void sessaoDeEstudante403() throws Exception {
        mvc.perform(put(ROTA + "x")
                        .cookie(Sessao.cookie(tokens, Sessao.USUARIO_TESTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("X", "prova", "2026-12-06", null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso restrito a administradores"));
        assertThat(usuarios.findById(Sessao.USUARIO_TESTE).orElseThrow().getPapel()).isEqualTo(Papel.ESTUDANTE);
    }

    @Test
    void emailDaListaViraAdminSemDiferenciarMaiusculas() throws Exception {
        UUID id = usuarios.saveAndFlush(new Usuario("Admin", "admin@sauf.local", null)).getId();
        assertThat(usuarios.findById(id).orElseThrow().getPapel()).isEqualTo(Papel.ESTUDANTE);

        mvc.perform(put(ROTA + "feira-uem")
                        .cookie(Sessao.cookie(tokens, id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evento("Feira de profissões", "feira", "2026-12-10", null, 57)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criados").value(1));
        assertThat(usuarios.findById(id).orElseThrow().getPapel()).isEqualTo(Papel.ADMIN);
    }
}
