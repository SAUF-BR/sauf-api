package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DocumentosLegaisApiTest {

    private static final String SENHA = "Segura@123";
    private static final String OS_DOIS = """
            [{"tipo": "termos-uso", "versao": "1.0.0"}, {"tipo": "politica-privacidade", "versao": "1.0.0"}]""";

    @Autowired MockMvc mvc;
    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    @Autowired JdbcTemplate jdbc;

    @Test
    void devolveAsVersoesVigentesDosDoisDocumentos() throws Exception {
        mvc.perform(get("/api/v1/documentos-legais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tipo", containsInAnyOrder("termos-uso", "politica-privacidade")))
                .andExpect(jsonPath("$[0].versao").value("1.0.0"))
                .andExpect(jsonPath("$[0].titulo").value("Termos de Uso do SAUF-BR"))
                .andExpect(jsonPath("$[0].vigenteDesde").value("2026-11-15"))
                .andExpect(jsonPath("$[0].minutosLeitura", greaterThan(0)))
                .andExpect(jsonPath("$[0].conteudo", containsString("# Termos de Uso do SAUF-BR")))
                .andExpect(jsonPath("$[0].sha256").isNotEmpty())
                .andExpect(jsonPath("$[1].titulo").value("Política de Privacidade do SAUF-BR"));
    }

    @Test
    void cadastroGuardaOAceiteComVersaoHashIpENavegador() throws Exception {
        cadastrar(OS_DOIS).andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentosPendentes", empty()));

        var aceites = jdbc.queryForList("""
                select d.tipo, d.versao, a.conteudo_sha256 = d.conteudo_sha256 as hash_confere, a.ip, a.user_agent, a.canal
                from aceite_documento a
                join documento_legal_versao d on d.id = a.documento_versao_id
                join usuario u on u.id = a.usuario_id
                where u.email = 'ana@exemplo.com'
                order by d.tipo""");
        assertThat(aceites).hasSize(2);
        assertThat(aceites).allSatisfy(aceite -> {
            assertThat(aceite.get("versao")).isEqualTo("1.0.0");
            assertThat(aceite.get("hash_confere")).isEqualTo(true);
            assertThat(aceite.get("ip")).isEqualTo("200.1.2.3");
            assertThat(aceite.get("user_agent")).isEqualTo("Navegador de teste");
            assertThat(aceite.get("canal")).isEqualTo("WEB");
        });
    }

    @Test
    void cadastroSemUmDosDocumentosResponde400() throws Exception {
        cadastrar("[{\"tipo\": \"termos-uso\", \"versao\": \"1.0.0\"}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.aceites").value("Aceite a Política de Privacidade para continuar"));
    }

    @Test
    void versaoAntigaNoCadastroResponde409ENaoCriaAConta() throws Exception {
        cadastrar("""
                [{"tipo": "termos-uso", "versao": "0.9.0"}, {"tipo": "politica-privacidade", "versao": "1.0.0"}]""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.aceites", containsString("Leia a nova versão")));

        assertThat(jdbc.queryForObject("select count(*) from usuario where email = 'ana@exemplo.com'", Integer.class))
                .isZero();
    }

    @Test
    void contaQueNaoAceitouOsDocumentosVemComPendenciasEAceitaDepois() throws Exception {
        MockMvc logado = Sessao.mvcLogado(contexto, tokens);

        logado.perform(get("/api/v1/me"))
                .andExpect(jsonPath("$.documentosPendentes", containsInAnyOrder("termos-uso", "politica-privacidade")));

        logado.perform(post("/api/v1/me/aceites").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceites\": " + OS_DOIS + "}"))
                .andExpect(status().isNoContent());

        logado.perform(get("/api/v1/me")).andExpect(jsonPath("$.documentosPendentes", empty()));
    }

    @Test
    void aceitarDeNovoNaoDuplicaORegistro() throws Exception {
        MockMvc logado = Sessao.mvcLogado(contexto, tokens);
        for (int i = 0; i < 2; i++) {
            logado.perform(post("/api/v1/me/aceites").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"aceites\": " + OS_DOIS + "}"))
                    .andExpect(status().isNoContent());
        }

        assertThat(jdbc.queryForObject(
                "select count(*) from aceite_documento where usuario_id = ?", Integer.class, Sessao.USUARIO_TESTE))
                .isEqualTo(2);
    }

    @Test
    void aceitarSemSessaoResponde401() throws Exception {
        mvc.perform(post("/api/v1/me/aceites").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceites\": " + OS_DOIS + "}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions cadastrar(String aceites) throws Exception {
        String corpo = """
                {"nome": "Ana Lima", "email": "ana@exemplo.com", "senha": "%s", "aceites": %s}
                """.formatted(SENHA, aceites);
        return mvc.perform(post("/api/v1/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "200.1.2.3, 10.0.0.1")
                .header("User-Agent", "Navegador de teste")
                .content(corpo));
    }
}
