package br.com.sauf_api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import java.time.LocalDate;
import java.time.ZoneOffset;
import br.com.sauf_api.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssinaturaApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    MockMvc mvc;

    @BeforeEach
    void entrar() {
        mvc = Sessao.mvcLogado(contexto, tokens);
    }
    @Autowired AssinaturaRepository assinaturas;

    @Test
    void listaPlanosDoMaisBaratoAoMaisCaro() throws Exception {
        mvc.perform(get("/api/v1/assinatura/planos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id", contains("basic", "plus", "premium")))
                .andExpect(jsonPath("$[0].precoMensal").value(8.0))
                .andExpect(jsonPath("$[0].recursos", contains("simulados-enem")))
                .andExpect(jsonPath("$[0].selo").value(nullValue()))
                .andExpect(jsonPath("$[2].selo").value("Completo"))
                .andExpect(jsonPath("$[2].recursos", contains(
                        "simulados-enem", "simulados-vestibulares", "dashboard", "timer", "ranking")));
    }

    @Test
    void semAssinaturaResponde204() throws Exception {
        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void assinarCriaAssinaturaAtivaPorUmMes() throws Exception {
        assinar("plus")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("plus"))
                .andExpect(jsonPath("$.status").value("ativa"))
                .andExpect(jsonPath("$.renovaEm").value(hoje().plusMonths(1).toString()));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("plus"));
    }

    @Test
    void trocarDePlanoMantemADataDeRenovacao() throws Exception {
        assinaturas.save(new Assinatura(Sessao.USUARIO_TESTE, Plano.BASIC, hoje().plusDays(10)));

        assinar("premium")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("premium"))
                .andExpect(jsonPath("$.renovaEm").value(hoje().plusDays(10).toString()));
    }

    @Test
    void escolherOPlanoAtualResponde409() throws Exception {
        assinar("plus");

        assinar("plus")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Você já está no plano Plus"));
    }

    @Test
    void cancelarEReativar() throws Exception {
        assinar("basic");

        mvc.perform(post("/api/v1/assinatura/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelamento-agendado"));

        mvc.perform(post("/api/v1/assinatura/reativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ativa"));
    }

    @Test
    void cancelarDuasVezesResponde409() throws Exception {
        assinar("basic");
        mvc.perform(post("/api/v1/assinatura/cancelar"));

        mvc.perform(post("/api/v1/assinatura/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarSemAssinaturaResponde409() throws Exception {
        mvc.perform(post("/api/v1/assinatura/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Você não tem uma assinatura ativa"));
    }

    @Test
    void assinaturaVencidaContaComoSemAssinatura() throws Exception {
        assinaturas.save(new Assinatura(Sessao.USUARIO_TESTE, Plano.PLUS, hoje().minusDays(1)));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isNoContent());
    }

    @Test
    void planoInexistenteResponde400() throws Exception {
        assinar("ouro")
                .andExpect(status().isBadRequest());
    }

    private ResultActions assinar(String plano) throws Exception {
        return mvc.perform(put("/api/v1/assinatura")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plano\": \"" + plano + "\"}"));
    }

    private LocalDate hoje() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
