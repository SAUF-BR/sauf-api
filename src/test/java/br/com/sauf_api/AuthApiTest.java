package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sauf_api.auth.CookieSessao;
import br.com.sauf_api.email.Email;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(CaixaDeEmailsTeste.Config.class)
@Transactional
class AuthApiTest {

    private static final String SENHA = "Segura@123";
    private static final String EMAIL = "ana@exemplo.com";

    @Autowired MockMvc mvc;
    @Autowired CaixaDeEmailsTeste caixa;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    @BeforeEach
    void esvaziarCaixa() {
        caixa.limpar();
    }

    // ---- Cadastro (D-85)

    @Test
    void cadastroCriaAContaEJaEntra() throws Exception {
        MvcResult resultado = cadastrar("  Ana Lima ", "Ana@Exemplo.com ", SENHA, true)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString(CookieSessao.NOME + "="),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"),
                        containsString("Path=/api"))))
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.emailVerificado").value(false))
                .andReturn();

        mvc.perform(get("/api/v1/me").cookie(sessao(resultado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
        // A verificação do e-mail ficou para depois (D-89): nada é enviado no cadastro
        assertThat(caixa.enviados()).isEmpty();
    }

    @Test
    void loginComASenhaCertaEntraMesmoSemEmailVerificado() throws Exception {
        criarConta();

        MvcResult resultado = entrar(" ANA@exemplo.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andReturn();

        mvc.perform(get("/api/v1/me").cookie(sessao(resultado))).andExpect(status().isOk());
    }

    // ---- Esqueci minha senha por código (D-90)

    @Test
    void codigoCertoEntraELiberaCriarASenhaNovaSemAAtual() throws Exception {
        criarConta();

        esqueci(EMAIL).andExpect(status().isNoContent());
        Email email = caixa.ultimo();
        assertThat(email.para()).isEqualTo(EMAIL);
        String codigo = caixa.codigoDoUltimo();
        assertThat(email.assunto()).isEqualTo("Seu código de acesso ao SAUF: " + codigo);
        assertThat(email.html()).contains(codigo);

        MvcResult resultado = entrarComCodigo(EMAIL, codigo)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(CookieSessao.NOME + "=")))
                .andExpect(jsonPath("$.emailVerificado").value(true))
                .andExpect(jsonPath("$.trocaSenhaSemAtualAte").isNotEmpty())
                .andReturn();
        Cookie sessao = sessao(resultado);

        mvc.perform(get("/api/v1/me").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trocaSenhaSemAtualAte").isNotEmpty());

        // Cria a senha nova sem informar a atual; a sessão é renovada sem a liberação
        MvcResult troca = mvc.perform(put("/api/v1/me/senha").cookie(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaSenha\": \"NovaSenha#1\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(CookieSessao.NOME + "=")))
                .andReturn();
        mvc.perform(get("/api/v1/me").cookie(sessao(troca)))
                .andExpect(jsonPath("$.trocaSenhaSemAtualAte").value(nullValue()));

        entrar(EMAIL, SENHA).andExpect(status().isUnauthorized());
        entrar(EMAIL, "NovaSenha#1").andExpect(status().isOk());
    }

    @Test
    void sessaoNormalContinuaPedindoASenhaAtual() throws Exception {
        criarConta();
        MvcResult login = entrar(EMAIL, SENHA).andReturn();

        mvc.perform(put("/api/v1/me/senha").cookie(sessao(login))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novaSenha\": \"NovaSenha#1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.senhaAtual").value("Informe sua senha atual"));
    }

    @Test
    void codigoErradoContaAsTentativasEDepoisDe5PedeUmNovo() throws Exception {
        criarConta();
        esqueci(EMAIL);
        String certo = caixa.codigoDoUltimo();
        String errado = certo.equals("000000") ? "111111" : "000000";

        entrarComCodigo(EMAIL, errado)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigo").value("Código incorreto. Você ainda tem 4 tentativas."));
        for (int i = 0; i < 3; i++) {
            entrarComCodigo(EMAIL, errado);
        }
        entrarComCodigo(EMAIL, errado)
                .andExpect(jsonPath("$.fields.codigo")
                        .value("Código incorreto. Você usou as 5 tentativas: peça um novo código."));

        // Nem o código certo vale mais
        entrarComCodigo(EMAIL, certo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigo").value("Código inválido ou vencido. Peça um novo código."));
    }

    @Test
    void codigoSoFuncionaUmaVez() throws Exception {
        criarConta();
        esqueci(EMAIL);
        String codigo = caixa.codigoDoUltimo();
        entrarComCodigo(EMAIL, codigo).andExpect(status().isOk());

        entrarComCodigo(EMAIL, codigo).andExpect(status().isBadRequest());
    }

    @Test
    void codigoVencidoResponde400() throws Exception {
        criarConta();
        esqueci(EMAIL);
        jdbc.update("update token_email set expira_em = ?", Timestamp.from(Instant.now().minusSeconds(1)));
        // O teste roda numa transação só: descarta o código guardado em memória pelo JPA
        em.clear();

        entrarComCodigo(EMAIL, caixa.codigoDoUltimo())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigo").value("Código inválido ou vencido. Peça um novo código."));
    }

    @Test
    void pedirDuasVezesSeguidasMandaUmSoEmail() throws Exception {
        criarConta();

        esqueci(EMAIL).andExpect(status().isNoContent());
        esqueci(EMAIL).andExpect(status().isNoContent());

        assertThat(caixa.enviados()).hasSize(1);
    }

    @Test
    void pedirDeNovoDepoisDeUmMinutoMandaCodigoNovoEOAnteriorDeixaDeValer() throws Exception {
        criarConta();
        esqueci(EMAIL);
        String primeiro = caixa.codigoDoUltimo();
        passarUmMinuto();

        esqueci(EMAIL);

        assertThat(caixa.enviados()).hasSize(2);
        String segundo = caixa.codigoDoUltimo();
        if (!primeiro.equals(segundo)) {
            entrarComCodigo(EMAIL, primeiro).andExpect(status().isBadRequest());
        }
        entrarComCodigo(EMAIL, segundo).andExpect(status().isOk());
    }

    @Test
    void emailSemContaResponde204SemEnviarECodigoNaoEntra() throws Exception {
        esqueci("ninguem@exemplo.com").andExpect(status().isNoContent());
        assertThat(caixa.enviados()).isEmpty();

        entrarComCodigo("ninguem@exemplo.com", "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigo").value("Código inválido ou vencido. Peça um novo código."));
    }

    @Test
    void codigoForaDoFormatoResponde400() throws Exception {
        entrarComCodigo(EMAIL, "12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.codigo").value("O código tem 6 números"));
    }

    // ---- E-mail disponível, conferido antes da etapa dos termos (D-94)

    @Test
    void emailSemContaEstaDisponivel() throws Exception {
        postar("/api/v1/auth/email-disponivel", "{\"email\": \"nova@exemplo.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(true));
    }

    @Test
    void emailJaCadastradoNaoEstaDisponivelSemDiferenciarMaiusculas() throws Exception {
        cadastrar("Ana Lima", EMAIL, SENHA, true);

        postar("/api/v1/auth/email-disponivel", "{\"email\": \" ANA@Exemplo.com \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(false));
    }

    @Test
    void emailInvalidoNaConferenciaResponde400() throws Exception {
        postar("/api/v1/auth/email-disponivel", "{\"email\": \"sem-arroba\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Informe um e-mail válido"));
    }

    // ---- Validações do cadastro

    @Test
    void emailJaCadastradoResponde409SemDiferenciarMaiusculas() throws Exception {
        cadastrar("Ana Lima", EMAIL, SENHA, true);

        cadastrar("Outra Ana", "ANA@exemplo.com", SENHA, true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fields.email").value("Já existe uma conta com este e-mail"));
    }

    @Test
    void senhaForaDaRegraResponde400ComOMotivo() throws Exception {
        cadastrar("Ana Lima", EMAIL, "Ab@1", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.senha").value("A senha deve ter no mínimo 8 caracteres"));
        cadastrar("Ana Lima", EMAIL, "12345678@", true)
                .andExpect(jsonPath("$.fields.senha").value("A senha precisa de pelo menos 1 letra"));
        cadastrar("Ana Lima", EMAIL, "SemEspecial1", true)
                .andExpect(jsonPath("$.fields.senha").value("A senha precisa de pelo menos 1 caractere especial"));
    }

    @Test
    void semAceitarOsTermosResponde400() throws Exception {
        cadastrar("Ana Lima", EMAIL, SENHA, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.aceites")
                        .value("Aceite os Termos de Uso e a Política de Privacidade para criar a conta"));
    }

    @Test
    void emailInvalidoResponde400() throws Exception {
        cadastrar("Ana Lima", "ana-sem-arroba", SENHA, true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Informe um e-mail válido"));
    }

    // ---- Login, logout e proteção das rotas (D-84, D-87)

    @Test
    void senhaErradaEEmailInexistenteTemAMesmaMensagem() throws Exception {
        criarConta();

        entrar(EMAIL, "Errada@123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"));
        entrar("ninguem@exemplo.com", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-mail ou senha incorretos"));
    }

    @Test
    void contaSemSenhaNaoEntra() throws Exception {
        entrar("teste@sauf.local", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutApagaOCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString(CookieSessao.NOME + "="),
                        containsString("Max-Age=0"))));
    }

    @Test
    void rotasDaContaSemSessaoRespondem401() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Entre na sua conta para continuar"));
        mvc.perform(get("/api/v1/me/foto")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/assinatura")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/assinatura/cancelar")).andExpect(status().isUnauthorized());
    }

    @Test
    void planosECatalogoContinuamPublicos() throws Exception {
        mvc.perform(get("/api/v1/assinatura/planos")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/areas")).andExpect(status().isOk());
    }

    @Test
    void cookieInvalidoNaoAtrapalhaRotaPublicaNemOLogin() throws Exception {
        Cookie adulterado = new Cookie(CookieSessao.NOME, "nao.e.um.token");
        criarConta();

        mvc.perform(get("/api/v1/areas").cookie(adulterado)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").cookie(adulterado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"senha\": \"" + SENHA + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me").cookie(adulterado)).andExpect(status().isUnauthorized());
    }

    // ---- Ajuda

    private void criarConta() throws Exception {
        cadastrar("Ana Lima", EMAIL, SENHA, true).andExpect(status().isCreated());
        caixa.limpar();
    }

    // Um novo código só sai depois de 1 minuto do último e-mail: "volta no tempo" os códigos já criados
    private void passarUmMinuto() {
        jdbc.update("update token_email set criado_em = ?", Timestamp.from(Instant.now().minusSeconds(120)));
    }

    // aceitouTermos = true manda o aceite das versões vigentes dos dois documentos (D-93)
    private ResultActions cadastrar(String nome, String email, String senha, boolean aceitouTermos) throws Exception {
        String aceites = aceitouTermos
                ? "[{\"tipo\": \"termos-uso\", \"versao\": \"1.0.0\"}, "
                        + "{\"tipo\": \"politica-privacidade\", \"versao\": \"1.0.0\"}]"
                : "[]";
        String corpo = """
                {"nome": "%s", "email": "%s", "senha": "%s", "aceites": %s}
                """.formatted(nome, email, senha, aceites);
        return postar("/api/v1/auth/cadastro", corpo);
    }

    private ResultActions entrar(String email, String senha) throws Exception {
        return postar("/api/v1/auth/login", "{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha));
    }

    private ResultActions esqueci(String email) throws Exception {
        return postar("/api/v1/auth/esqueci-senha", "{\"email\": \"%s\"}".formatted(email));
    }

    private ResultActions entrarComCodigo(String email, String codigo) throws Exception {
        return postar(
                "/api/v1/auth/esqueci-senha/codigo",
                "{\"email\": \"%s\", \"codigo\": \"%s\"}".formatted(email, codigo));
    }

    private ResultActions postar(String rota, String corpo) throws Exception {
        return mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static Cookie sessao(MvcResult resultado) {
        Cookie cookie = resultado.getResponse().getCookie(CookieSessao.NOME);
        assertThat(cookie).isNotNull();
        return new Cookie(CookieSessao.NOME, cookie.getValue());
    }
}
