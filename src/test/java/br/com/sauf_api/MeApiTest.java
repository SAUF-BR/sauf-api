package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.TokenService;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};

    @Autowired WebApplicationContext contexto;
    @Autowired TokenService tokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;
    MockMvc mvc;

    @BeforeEach
    void entrar() {
        mvc = Sessao.mvcLogado(contexto, tokens);
    }

    // ---- Troca de senha (D-91)

    @Test
    void trocaASenhaComASenhaAtualCerta() throws Exception {
        MockMvc comSenha = Sessao.mvcLogado(contexto, tokens, contaComSenha("Atual@123"));

        trocarSenha(comSenha, "Atual@123", "NovaSenha#1").andExpect(status().isNoContent());

        assertThat(senhas.matches("NovaSenha#1", usuarios.findByEmail("senha@exemplo.com").orElseThrow().getSenhaHash()))
                .isTrue();
        comSenha.perform(get("/api/v1/me")).andExpect(jsonPath("$.senhaAlteradaEm").isNotEmpty());
    }

    @Test
    void senhaAtualErradaResponde400NoCampo() throws Exception {
        MockMvc comSenha = Sessao.mvcLogado(contexto, tokens, contaComSenha("Atual@123"));

        trocarSenha(comSenha, "Errada@123", "NovaSenha#1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.senhaAtual").value("Senha atual incorreta"));
    }

    @Test
    void novaSenhaForaDaRegraOuIgualAAtualResponde400() throws Exception {
        MockMvc comSenha = Sessao.mvcLogado(contexto, tokens, contaComSenha("Atual@123"));

        trocarSenha(comSenha, "Atual@123", "curta@1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.novaSenha").value("A senha deve ter no mínimo 8 caracteres"));
        trocarSenha(comSenha, "Atual@123", "Atual@123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.novaSenha").value("A nova senha deve ser diferente da atual"));
    }

    @Test
    void contaSemSenhaNaoTrocaASenha() throws Exception {
        trocarSenha(mvc, "Qualquer@1", "NovaSenha#1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.senhaAtual").value("Senha atual incorreta"));
    }

    private UUID contaComSenha(String senha) {
        Usuario usuario = new Usuario("Conta Com Senha", "senha@exemplo.com", senhas.encode(senha));
        usuario.confirmarEmail();
        return usuarios.saveAndFlush(usuario).getId();
    }

    private static ResultActions trocarSenha(MockMvc mvc, String atual, String nova) throws Exception {
        return mvc.perform(put("/api/v1/me/senha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"%s\", \"novaSenha\": \"%s\"}".formatted(atual, nova)));
    }

    // ---- Perfil e foto (D-80, D-81)

    @Test
    void devolveOUsuarioProvisorioSemFoto() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Sessao.USUARIO_TESTE.toString()))
                .andExpect(jsonPath("$.nome").value("Usuário de teste"))
                .andExpect(jsonPath("$.email").value("teste@sauf.local"))
                .andExpect(jsonPath("$.emailVerificado").value(true))
                .andExpect(jsonPath("$.notaEnem").value(nullValue()))
                .andExpect(jsonPath("$.fotoUrl").value(nullValue()))
                .andExpect(jsonPath("$.senhaAlteradaEm").value(nullValue()));
    }

    @Test
    void atualizaNomeENotaSemEspacosNasPontas() throws Exception {
        atualizar("{\"nome\": \"  Heloísa Scarante \", \"notaEnem\": 698.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Heloísa Scarante"))
                .andExpect(jsonPath("$.notaEnem").value(698.5));

        mvc.perform(get("/api/v1/me"))
                .andExpect(jsonPath("$.nome").value("Heloísa Scarante"))
                .andExpect(jsonPath("$.notaEnem").value(698.5));
    }

    @Test
    void notaVaziaApagaANota() throws Exception {
        atualizar("{\"nome\": \"Heloísa\", \"notaEnem\": 700}");

        atualizar("{\"nome\": \"Heloísa\", \"notaEnem\": null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaEnem").value(nullValue()));
    }

    @Test
    void nomeCurtoResponde400ComOCampo() throws Exception {
        atualizar("{\"nome\": \"  ab \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nome").value("Informe seu nome completo"));
    }

    @Test
    void notaForaDaFaixaResponde400() throws Exception {
        atualizar("{\"nome\": \"Heloísa\", \"notaEnem\": 1000.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.notaEnem").value("Informe uma nota entre 0 e 1000"));
    }

    @Test
    void enviaTrocaERemoveAFoto() throws Exception {
        enviarFoto(PNG, "image/png")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl", startsWith("/api/v1/me/foto?v=")));

        mvc.perform(get("/api/v1/me/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", startsWith("max-age=")))
                .andExpect(content().bytes(PNG));

        enviarFoto(JPEG, "image/jpeg").andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/foto"))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(JPEG));

        mvc.perform(delete("/api/v1/me/foto")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/foto")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me")).andExpect(jsonPath("$.fotoUrl").value(nullValue()));
    }

    @Test
    void removerSemFotoResponde204() throws Exception {
        mvc.perform(delete("/api/v1/me/foto")).andExpect(status().isNoContent());
    }

    @Test
    void arquivoQueNaoEImagemResponde400MesmoComTipoDeImagem() throws Exception {
        enviarFoto("<script>alert(1)</script>".getBytes(), "image/png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.foto").value("Escolha um arquivo de imagem (JPG, PNG ou WebP)."));
    }

    @Test
    void fotoMaiorQue2MbResponde400() throws Exception {
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        enviarFoto(grande, "image/png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.foto").value("A foto deve ter no máximo 2 MB."));
    }

    @Test
    void semArquivoResponde400() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/me/foto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.foto").value("arquivo obrigatório"));
    }

    private ResultActions atualizar(String corpo) throws Exception {
        return mvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions enviarFoto(byte[] conteudo, String tipo) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/v1/me/foto")
                .file(new MockMultipartFile("foto", "foto", tipo, conteudo)));
    }
}
