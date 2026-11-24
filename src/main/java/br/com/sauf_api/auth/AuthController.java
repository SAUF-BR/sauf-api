package br.com.sauf_api.auth;

import br.com.sauf_api.documento.OrigemAceite;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioResposta;
import br.com.sauf_api.usuario.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login, logout e "esqueci minha senha" por código (D-84 a D-90). A sessão vai no cookie HttpOnly. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    // Depois de entrar pelo código, por quanto tempo a senha nova dispensa a atual (D-90)
    static final Duration TROCA_SENHA_SEM_ATUAL = Duration.ofMinutes(15);

    private final AuthService auth;
    private final TokenService tokens;
    private final CookieSessao cookie;
    private final UsuarioService usuarios;

    public AuthController(AuthService auth, TokenService tokens, CookieSessao cookie, UsuarioService usuarios) {
        this.auth = auth;
        this.tokens = tokens;
        this.cookie = cookie;
        this.usuarios = usuarios;
    }

    // A conta nova já entra (D-85)
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResposta> cadastrar(
            @Valid @RequestBody CadastroRequest request, HttpServletRequest http) {
        return comSessao(HttpStatus.CREATED, auth.cadastrar(request, OrigemAceite.de(http)));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResposta> entrar(@Valid @RequestBody LoginRequest request) {
        return comSessao(HttpStatus.OK, auth.entrar(request));
    }

    // Público de propósito: apagar o cookie funciona mesmo com a sessão já vencida
    @PostMapping("/logout")
    public ResponseEntity<Void> sair() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.apagar().toString())
                .build();
    }

    /**
     * Antes da etapa dos termos, o cadastro confere se o e-mail já tem conta (D-94). POST com o
     * e-mail no corpo, para ele não aparecer em URL nem em log de acesso.
     */
    @PostMapping("/email-disponivel")
    public EmailDisponivelResposta emailDisponivel(@Valid @RequestBody EmailRequest request) {
        return new EmailDisponivelResposta(auth.emailDisponivel(request.email()));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EmailRequest request) {
        auth.esqueciSenha(request.email());
        return ResponseEntity.noContent().build();
    }

    // Código certo: entra e pode criar a senha nova no perfil sem a atual por 15 minutos (D-90)
    @PostMapping("/esqueci-senha/codigo")
    public ResponseEntity<UsuarioResposta> entrarComCodigo(@Valid @RequestBody CodigoAcessoRequest request) {
        Usuario usuario = auth.entrarComCodigo(request);
        Instant trocaAte = Instant.now().plus(TROCA_SENHA_SEM_ATUAL);
        String token = tokens.gerar(usuario.getId(), trocaAte);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(token, tokens.duracao()).toString())
                .body(usuarios.buscar(usuario.getId()).comTrocaSenhaSemAtualAte(trocaAte));
    }

    private ResponseEntity<UsuarioResposta> comSessao(HttpStatus status, Usuario usuario) {
        String token = tokens.gerar(usuario.getId());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.criar(token, tokens.duracao()).toString())
                .body(usuarios.buscar(usuario.getId()));
    }
}
