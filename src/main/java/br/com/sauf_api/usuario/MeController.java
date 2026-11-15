package br.com.sauf_api.usuario;

import br.com.sauf_api.auth.AuthService;
import br.com.sauf_api.auth.CookieSessao;
import br.com.sauf_api.auth.TokenService;
import br.com.sauf_api.comum.UsuarioLogado;
import br.com.sauf_api.documento.AceitesRequest;
import br.com.sauf_api.documento.DocumentosLegaisService;
import br.com.sauf_api.documento.OrigemAceite;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Conta de quem está logado (D-80). Nenhuma rota recebe id: só se altera a própria conta. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UsuarioService service;
    private final AuthService auth;
    private final TokenService tokens;
    private final CookieSessao cookie;
    private final DocumentosLegaisService documentos;
    private final UsuarioLogado usuarioLogado;

    public MeController(
            UsuarioService service,
            AuthService auth,
            TokenService tokens,
            CookieSessao cookie,
            DocumentosLegaisService documentos,
            UsuarioLogado usuarioLogado) {
        this.service = service;
        this.auth = auth;
        this.tokens = tokens;
        this.cookie = cookie;
        this.documentos = documentos;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping
    public UsuarioResposta me() {
        return comSessao(service.buscar(usuarioLogado.id()));
    }

    @PatchMapping
    public UsuarioResposta atualizar(@Valid @RequestBody AtualizarPerfilRequest request) {
        return comSessao(service.atualizarPerfil(usuarioLogado.id(), request));
    }

    /**
     * Troca de senha (D-91). Logo depois de entrar pelo código do "esqueci minha senha" não pede
     * a senha atual (D-90); feita a troca, a sessão é renovada sem essa liberação.
     * O e-mail da conta não pode ser trocado (D-82).
     */
    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        boolean semSenhaAtual = usuarioLogado.podeTrocarSenhaSemAtual();
        auth.alterarSenha(usuarioLogado.id(), request, semSenhaAtual);
        if (!semSenhaAtual) {
            return ResponseEntity.noContent().build();
        }
        String token = tokens.gerar(usuarioLogado.id());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(token, tokens.duracao()).toString())
                .build();
    }

    // Aceite de uma versão nova dos documentos legais (tela "Atualizamos nossos termos", D-93)
    @PostMapping("/aceites")
    public ResponseEntity<Void> aceitar(@Valid @RequestBody AceitesRequest request, HttpServletRequest http) {
        documentos.registrar(usuarioLogado.id(), request.aceites(), OrigemAceite.de(http), false);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResposta enviarFoto(@RequestPart("foto") MultipartFile foto) {
        return comSessao(service.salvarFoto(usuarioLogado.id(), foto));
    }

    @DeleteMapping("/foto")
    public ResponseEntity<Void> removerFoto() {
        service.removerFoto(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    // A fotoUrl muda a cada troca (?v=), então a imagem pode ficar em cache no navegador
    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto() {
        return service.foto(usuarioLogado.id())
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.getTipo()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                        .body(foto.getConteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Informa ao front se a senha nova ainda dispensa a atual (entrou pelo código, D-90)
    private UsuarioResposta comSessao(UsuarioResposta resposta) {
        return usuarioLogado.podeTrocarSenhaSemAtual()
                ? resposta.comTrocaSenhaSemAtualAte(usuarioLogado.sessao().trocaSenhaSemAtualAte())
                : resposta;
    }
}
