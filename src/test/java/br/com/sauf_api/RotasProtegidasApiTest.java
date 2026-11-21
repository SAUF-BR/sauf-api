package br.com.sauf_api;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

/** Quem pode acessar cada rota (D-87, D-108). */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class RotasProtegidasApiTest {

    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @Autowired MockMvc mvc;
    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    MockMvc logado;

    @BeforeEach
    void entrar() {
        logado = Sessao.mvcLogado(contexto, tokens);
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "GET, /api/v1/simulados",
        "POST, /api/v1/simulados",
        "GET, /api/v1/simulados/opcoes",
        "GET, /api/v1/simulados/resumo",
        "GET, /api/v1/simulados/cronometro",
        "GET, /api/v1/simulados/acompanhamento",
        "GET, /api/v1/simulados/ranking",
        "GET, /api/v1/simulados/" + ID,
        "PUT, /api/v1/simulados/" + ID + "/questoes/1",
        "POST, /api/v1/simulados/" + ID + "/finalizar",
        "GET, /api/v1/simulados/" + ID + "/revisao",
        "POST, /api/v1/simulados/" + ID + "/refazer-erros",
        "GET, /api/v1/favoritos/cursos",
        "GET, /api/v1/favoritos/cursos/ids",
        "POST, /api/v1/favoritos/cursos/" + ID,
        "DELETE, /api/v1/favoritos/cursos/" + ID,
        "GET, /api/v1/favoritos/universidades",
        "GET, /api/v1/favoritos/universidades/ids",
        "POST, /api/v1/favoritos/universidades/" + ID,
        "DELETE, /api/v1/favoritos/universidades/" + ID,
        "GET, /api/v1/me",
        "PATCH, /api/v1/me",
        "PUT, /api/v1/me/senha",
        "POST, /api/v1/me/aceites",
        "GET, /api/v1/me/foto",
        "PUT, /api/v1/me/foto",
        "DELETE, /api/v1/me/foto",
        "GET, /api/v1/assinatura",
        "PUT, /api/v1/assinatura",
        "POST, /api/v1/assinatura/cancelar",
        "POST, /api/v1/assinatura/reativar",
    })
    void semSessaoResponde401(String metodo, String caminho) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(metodo), caminho)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Entre na sua conta para continuar"));
    }

    @ParameterizedTest(name = "GET {0}")
    @CsvSource({
        "/api/v1/simulados",
        "/api/v1/simulados/opcoes",
        "/api/v1/simulados/resumo",
        "/api/v1/simulados/cronometro",
        "/api/v1/simulados/acompanhamento",
        "/api/v1/simulados/ranking",
        "/api/v1/favoritos/cursos",
        "/api/v1/favoritos/cursos/ids",
        "/api/v1/favoritos/universidades",
        "/api/v1/favoritos/universidades/ids",
        "/api/v1/me",
    })
    void comSessaoResponde200(String caminho) throws Exception {
        logado.perform(get(caminho)).andExpect(status().isOk());
    }

    @Test
    void comSessaoChegaAoControllerNasRotasComId() throws Exception {
        // Simulado e curso inexistentes: quem responde é a regra da rota (404), não o login
        logado.perform(get("/api/v1/simulados/" + ID)).andExpect(status().isNotFound());
        logado.perform(get("/api/v1/simulados/" + ID + "/revisao")).andExpect(status().isNotFound());
        logado.perform(post("/api/v1/favoritos/cursos/" + ID)).andExpect(status().is(not(401)));
    }

    @ParameterizedTest(name = "GET {0}")
    @CsvSource({
        "/api/v1/cursos",
        "/api/v1/universidades",
        "/api/v1/universidades/contagens",
        "/api/v1/universidades/cidades",
        "/api/v1/areas",
        "/api/v1/documentos-legais",
        "/api/v1/assinatura/planos",
        "/api/v1/docs",
    })
    void rotasPublicasRespondem200SemSessao(String caminho) throws Exception {
        mvc.perform(get(caminho)).andExpect(status().isOk());
    }

    @Test
    void loginECadastroContinuamPublicos() throws Exception {
        mvc.perform(post("/api/v1/auth/email-disponivel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nova@exemplo.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ingestaoContinuaExigindoAApiKeyMesmoComSessao() throws Exception {
        mvc.perform(post("/api/v1/ingestao/lotes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        logado.perform(post("/api/v1/ingestao/lotes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
