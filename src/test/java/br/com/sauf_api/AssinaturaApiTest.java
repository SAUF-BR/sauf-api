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
import br.com.sauf_api.auth.TokenService;
import java.time.LocalDate;
import java.time.ZoneId;
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
                .andExpect(jsonPath("$[*].id", contains("free", "basic", "plus")))
                .andExpect(jsonPath("$[0].precoMensal").value(0.0))
                .andExpect(jsonPath("$[0].recursos", contains(
                        "busca-cursos", "busca-universidades", "teste-vocacional", "calendario", "favoritos")))
                .andExpect(jsonPath("$[1].precoMensal").value(8.0))
                .andExpect(jsonPath("$[1].recursos", contains(
                        "busca-cursos", "busca-universidades", "teste-vocacional", "calendario", "favoritos",
                        "simulados-enem")))
                .andExpect(jsonPath("$[1].selo").value(nullValue()))
                .andExpect(jsonPath("$[2].precoMensal").value(10.0))
                .andExpect(jsonPath("$[2].selo").value("Completo"))
                .andExpect(jsonPath("$[2].recursos", contains(
                        "busca-cursos", "busca-universidades", "teste-vocacional", "calendario", "favoritos",
                        "simulados-enem", "ranking", "timer", "dashboard")));
    }

    @Test
    void assinarOFreeResponde409ENaoGrava() throws Exception {
        assinar("free")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "O plano Free não precisa ser assinado. Para voltar a ele, cancele a sua assinatura"));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isNoContent());
    }

    @Test
    void semAssinaturaResponde204() throws Exception {
        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void putNaoAtivaAssinaturaSemPagamento() throws Exception {
        assinar("plus")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Para assinar, use o checkout"));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isNoContent());
    }

    @Test
    void putNaoMudaAssinaturaExistente() throws Exception {
        assinaturaAtiva();

        assinar("plus")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Para assinar, use o checkout"));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("basic"))
                .andExpect(jsonPath("$.status").value("ativa"))
                .andExpect(jsonPath("$.renovaEm").value(hoje().plusDays(10).toString()));
    }

    @Test
    void putComAssinaturaNoStripeIndicaOPortalENaoMudaOPlano() throws Exception {
        assinaturaAtiva().vincularAoStripe("sub_1");

        assinar("plus")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Para trocar de plano, use o portal da assinatura"));

        mvc.perform(get("/api/v1/assinatura"))
                .andExpect(jsonPath("$.plano").value("basic"));
    }

    @Test
    void cancelarEReativar() throws Exception {
        assinaturaAtiva();

        mvc.perform(post("/api/v1/assinatura/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelamento-agendado"));

        mvc.perform(post("/api/v1/assinatura/reativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ativa"));
    }

    @Test
    void cancelarDuasVezesResponde409() throws Exception {
        assinaturaAtiva();
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

    private Assinatura assinaturaAtiva() {
        return assinaturas.save(new Assinatura(Sessao.USUARIO_TESTE, Plano.BASIC, hoje().plusDays(10)));
    }

    private ResultActions assinar(String plano) throws Exception {
        return mvc.perform(put("/api/v1/assinatura")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plano\": \"" + plano + "\"}"));
    }

    private LocalDate hoje() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }
}
