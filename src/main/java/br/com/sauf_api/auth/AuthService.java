package br.com.sauf_api.auth;

import br.com.sauf_api.comum.CampoInvalidoException;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.documento.DocumentosLegaisService;
import br.com.sauf_api.documento.OrigemAceite;
import br.com.sauf_api.email.EnvioEmail;
import br.com.sauf_api.email.ModelosEmail;
import br.com.sauf_api.usuario.AlterarSenhaRequest;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final CodigoAcessoService codigos;
    private final ModelosEmail modelos;
    private final EnvioEmail envio;
    private final DocumentosLegaisService documentos;
    // Conferido quando o e-mail não existe, para a resposta levar o mesmo tempo
    private final String hashFalso;

    public AuthService(
            UsuarioRepository usuarios,
            PasswordEncoder senhas,
            CodigoAcessoService codigos,
            ModelosEmail modelos,
            EnvioEmail envio,
            DocumentosLegaisService documentos) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.codigos = codigos;
        this.modelos = modelos;
        this.envio = envio;
        this.documentos = documentos;
        this.hashFalso = senhas.encode("senha-que-nao-existe-@1");
    }

    /**
     * Cria a conta (D-85) e registra os aceites dos documentos legais vigentes (D-93): se a tela
     * mostrou uma versão antiga, nada é criado (409). A verificação do e-mail ficou para depois (D-89).
     */
    @Transactional
    public Usuario cadastrar(CadastroRequest request, OrigemAceite origem) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarios.existsByEmail(email)) {
            throw new EmailEmUsoException();
        }
        // Documentos desatualizados ou faltando: recusa antes de criar a conta
        documentos.conferir(request.aceites(), true);
        Usuario usuario;
        try {
            usuario = usuarios.saveAndFlush(new Usuario(request.nome(), email, senhas.encode(request.senha())));
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros com o mesmo e-mail ao mesmo tempo: o UNIQUE do banco decide
            throw new EmailEmUsoException();
        }
        documentos.registrar(usuario.getId(), request.aceites(), origem, true);
        return usuario;
    }

    /** Se ainda dá para criar conta com o e-mail (sem diferenciar maiúsculas). */
    @Transactional(readOnly = true)
    public boolean emailDisponivel(String email) {
        return !usuarios.existsByEmail(Usuario.normalizarEmail(email));
    }

    @Transactional(readOnly = true)
    public Usuario entrar(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(request.email())).orElse(null);
        String hash = usuario == null || usuario.getSenhaHash() == null ? hashFalso : usuario.getSenhaHash();
        boolean confere = senhas.matches(request.senha(), hash);
        if (usuario == null || usuario.getSenhaHash() == null || !confere) {
            throw new CredenciaisInvalidasException();
        }
        return usuario;
    }

    /**
     * Manda o código de 6 dígitos (D-90). Sempre "sucesso" para quem chama: não revela se
     * o e-mail tem conta. No máximo um e-mail por minuto para a mesma conta.
     */
    @Transactional
    public void esqueciSenha(String email) {
        usuarios.findByEmail(Usuario.normalizarEmail(email))
                .filter(usuario -> !codigos.pedidoRecente(usuario.getId(), TipoTokenEmail.REDEFINICAO_SENHA))
                .ifPresent(usuario -> {
                    String codigo = codigos.criar(usuario.getId(), TipoTokenEmail.REDEFINICAO_SENHA);
                    envio.enviar(modelos.codigoAcesso(usuario.getEmail(), usuario.getNome(), codigo));
                });
    }

    /**
     * Código certo: a pessoa entra (quem chama abre a sessão) e pode criar uma senha nova no
     * perfil sem a atual (D-90). Receber o código prova o e-mail, então ele fica verificado.
     * E-mail sem conta responde igual a código inválido (não revela quais e-mails existem).
     */
    @Transactional(noRollbackFor = CampoInvalidoException.class)
    public Usuario entrarComCodigo(CodigoAcessoRequest request) {
        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(request.email()))
                .orElseThrow(() -> new CampoInvalidoException("codigo", CodigoAcessoService.MENSAGEM_INVALIDO));
        codigos.conferir(usuario.getId(), TipoTokenEmail.REDEFINICAO_SENHA, request.codigo());
        usuario.confirmarEmail();
        return usuario;
    }

    /**
     * Troca de senha pelo perfil (D-91). Pede a senha atual, menos logo depois de entrar
     * pelo código do "esqueci minha senha" (D-90), quando a pessoa não sabe a atual.
     */
    @Transactional
    public void alterarSenha(UUID usuarioId, AlterarSenhaRequest request, boolean semSenhaAtual) {
        Usuario usuario = buscar(usuarioId);
        if (!semSenhaAtual) {
            String atual = request.senhaAtual();
            if (atual == null || atual.isBlank()) {
                throw new CampoInvalidoException("senhaAtual", "Informe sua senha atual");
            }
            if (usuario.getSenhaHash() == null || !senhas.matches(atual, usuario.getSenhaHash())) {
                throw new CampoInvalidoException("senhaAtual", "Senha atual incorreta");
            }
        }
        if (usuario.getSenhaHash() != null && senhas.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new CampoInvalidoException("novaSenha", "A nova senha deve ser diferente da atual");
        }
        usuario.trocarSenha(senhas.encode(request.novaSenha()));
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", usuarioId));
    }
}
