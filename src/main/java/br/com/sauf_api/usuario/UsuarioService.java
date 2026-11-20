package br.com.sauf_api.usuario;

import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.comum.TipoImagem;
import br.com.sauf_api.documento.DocumentosLegaisService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UsuarioService {

    static final long TAMANHO_MAXIMO_FOTO = 2L * 1024 * 1024;

    private final UsuarioRepository usuarios;
    private final FotoUsuarioRepository fotos;
    private final DocumentosLegaisService documentos;

    public UsuarioService(UsuarioRepository usuarios, FotoUsuarioRepository fotos, DocumentosLegaisService documentos) {
        this.usuarios = usuarios;
        this.fotos = fotos;
        this.documentos = documentos;
    }

    @Transactional(readOnly = true)
    public UsuarioResposta buscar(UUID usuarioId) {
        return responder(buscarUsuario(usuarioId));
    }

    @Transactional
    public UsuarioResposta atualizarPerfil(UUID usuarioId, AtualizarPerfilRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);
        usuario.atualizarPerfil(request.nome(), request.notaEnem());
        return responder(usuario);
    }

    @Transactional
    public UsuarioResposta salvarFoto(UUID usuarioId, MultipartFile arquivo) {
        Usuario usuario = buscarUsuario(usuarioId);
        byte[] conteudo = ler(arquivo);
        String tipo = TipoImagem.detectar(conteudo)
                .orElseThrow(() -> new FotoInvalidaException("Escolha um arquivo de imagem (JPG, PNG ou WebP)."));

        fotos.findById(usuarioId).ifPresentOrElse(
                foto -> foto.trocar(conteudo, tipo),
                () -> fotos.save(new FotoUsuario(usuarioId, conteudo, tipo)));
        // Grava antes de montar a resposta, que lê a data da foto no banco
        fotos.flush();
        return responder(usuario);
    }

    @Transactional
    public void removerFoto(UUID usuarioId) {
        buscarUsuario(usuarioId);
        fotos.findById(usuarioId).ifPresent(fotos::delete);
    }

    @Transactional(readOnly = true)
    public Optional<FotoUsuario> foto(UUID usuarioId) {
        return fotos.findById(usuarioId);
    }

    private byte[] ler(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new FotoInvalidaException("Escolha uma foto para enviar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_FOTO) {
            throw new FotoInvalidaException("A foto deve ter no máximo 2 MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a foto enviada", e);
        }
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", usuarioId));
    }

    private UsuarioResposta responder(Usuario usuario) {
        return UsuarioResposta.de(
                usuario,
                fotos.buscarAtualizadaEm(usuario.getId()).orElse(null),
                documentos.pendentes(usuario.getId()));
    }
}
