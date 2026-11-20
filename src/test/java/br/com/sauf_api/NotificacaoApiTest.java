package br.com.sauf_api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import br.com.sauf_api.notificacao.Notificacao;
import br.com.sauf_api.notificacao.NotificacaoRepository;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificacaoApiTest {

    static final String ROTA = "/api/v1/notificacoes";

    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    @Autowired NotificacaoRepository notificacoes;
    @Autowired UsuarioRepository usuarios;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = Sessao.mvcLogado(contexto, tokens);
    }

    @Test
    void semNotificacoesListaFicaVaziaEContagemZero() throws Exception {
        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get(ROTA + "/nao-lidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void listaDoMaisRecenteAoMaisAntigoComAcaoOpcional() throws Exception {
        Instant agora = Instant.now();
        Notificacao antiga = salvar(Sessao.USUARIO_TESTE, "Bem-vindo",
                "Complete seu perfil", "Ir para o perfil", "/perfil", agora.minusSeconds(3600));
        Notificacao nova = salvar(Sessao.USUARIO_TESTE, "Prazo do ENEM",
                "Encerra em 7 dias", null, null, agora.minusSeconds(60));

        mvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(nova.getId().toString()))
                .andExpect(jsonPath("$[0].titulo").value("Prazo do ENEM"))
                .andExpect(jsonPath("$[0].descricao").value("Encerra em 7 dias"))
                .andExpect(jsonPath("$[0].lida").value(false))
                .andExpect(jsonPath("$[0].acao").value(nullValue()))
                .andExpect(jsonPath("$[1].id").value(antiga.getId().toString()))
                .andExpect(jsonPath("$[1].acao.rotulo").value("Ir para o perfil"))
                .andExpect(jsonPath("$[1].acao.para").value("/perfil"));
    }

    @Test
    void contagemConsideraSoAsNaoLidas() throws Exception {
        salvar(Sessao.USUARIO_TESTE, "Aviso A", "x", null, null, Instant.now());
        Notificacao lida = salvar(Sessao.USUARIO_TESTE, "Aviso B", "y", null, null, Instant.now());
        lida.marcarComoLida(Instant.now());
        notificacoes.saveAndFlush(lida);

        mvc.perform(get(ROTA + "/nao-lidas"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void marcarTodasComoLidasZeraContagemEMarcaTodas() throws Exception {
        salvar(Sessao.USUARIO_TESTE, "A", "x", null, null, Instant.now().minusSeconds(60));
        salvar(Sessao.USUARIO_TESTE, "B", "y", null, null, Instant.now());

        mvc.perform(patch(ROTA + "/marcar-todas-como-lidas"))
                .andExpect(status().isNoContent());

        mvc.perform(get(ROTA + "/nao-lidas"))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].lida").value(true))
                .andExpect(jsonPath("$[1].lida").value(true));
    }

    @Test
    void marcarTodasComoLidasSemNotificacoesEhIdempotente() throws Exception {
        mvc.perform(patch(ROTA + "/marcar-todas-como-lidas"))
                .andExpect(status().isNoContent());
        mvc.perform(patch(ROTA + "/marcar-todas-como-lidas"))
                .andExpect(status().isNoContent());
    }

    @Test
    void notificacaoDeOutroUsuarioNaoAparece() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Outra pessoa", "outra@sauf.local", null));
        salvar(outro.getId(), "Para outro", "não deve aparecer", null, null, Instant.now());
        salvar(Sessao.USUARIO_TESTE, "Para mim", "deve aparecer", null, null, Instant.now());

        mvc.perform(get(ROTA))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Para mim"));

        mvc.perform(get(ROTA + "/nao-lidas"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void marcarTodasComoLidasNaoAfetaOutroUsuario() throws Exception {
        Usuario outro = usuarios.save(new Usuario("Outra pessoa", "outra@sauf.local", null));
        salvar(outro.getId(), "Para outro", "x", null, null, Instant.now());
        salvar(Sessao.USUARIO_TESTE, "Para mim", "y", null, null, Instant.now());

        mvc.perform(patch(ROTA + "/marcar-todas-como-lidas"))
                .andExpect(status().isNoContent());

        MockMvc mvcOutro = Sessao.mvcLogado(contexto, tokens, outro.getId());
        mvcOutro.perform(get(ROTA + "/nao-lidas"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void semSessaoResponde401() throws Exception {
        MockMvc anonimo = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        anonimo.perform(get(ROTA))
                .andExpect(status().isUnauthorized());
        anonimo.perform(get(ROTA + "/nao-lidas"))
                .andExpect(status().isUnauthorized());
        anonimo.perform(patch(ROTA + "/marcar-todas-como-lidas"))
                .andExpect(status().isUnauthorized());
    }

    private Notificacao salvar(
            UUID usuarioId, String titulo, String descricao, String rotulo, String para, Instant criadaEm) {
        Notificacao n = new Notificacao(usuarioId, titulo, descricao, rotulo, para);
        // O construtor usa Instant.now(); forçamos um instante específico para os cenários de ordenação.
        try {
            var campo = Notificacao.class.getDeclaredField("criadaEm");
            campo.setAccessible(true);
            campo.set(n, criadaEm);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return notificacoes.saveAndFlush(n);
    }
}
