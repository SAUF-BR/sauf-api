package br.com.sauf_api.usuario;

import br.com.sauf_api.documento.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UsuarioResposta(
        UUID id,
        String nome,
        String email,
        boolean emailVerificado,
        BigDecimal notaEnem,
        @Schema(description = "Caminho da foto, com ?v= que muda a cada troca (evita cache antigo); null sem foto",
                example = "/api/v1/me/foto?v=1731196800000")
        String fotoUrl,
        Instant criadoEm,
        Instant senhaAlteradaEm,
        @Schema(description = "Preenchido logo depois de entrar pelo código do \"esqueci minha senha\": até "
                + "esse instante a senha nova pode ser criada sem a atual (D-90); null no resto do tempo")
        Instant trocaSenhaSemAtualAte,
        @Schema(description = "Documentos legais com versão nova que a pessoa ainda precisa aceitar (D-93); "
                + "vazio quando está em dia")
        List<TipoDocumento> documentosPendentes) {

    static final String CAMINHO_FOTO = "/api/v1/me/foto";

    public UsuarioResposta comTrocaSenhaSemAtualAte(Instant ate) {
        return new UsuarioResposta(
                id, nome, email, emailVerificado, notaEnem, fotoUrl, criadoEm, senhaAlteradaEm, ate, documentosPendentes);
    }

    public static UsuarioResposta de(Usuario usuario, Instant fotoAtualizadaEm, List<TipoDocumento> pendentes) {
        String fotoUrl = fotoAtualizadaEm == null ? null : CAMINHO_FOTO + "?v=" + fotoAtualizadaEm.toEpochMilli();
        return new UsuarioResposta(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.isEmailVerificado(),
                usuario.getNotaEnem(),
                fotoUrl,
                usuario.getCriadoEm(),
                usuario.getSenhaAlteradaEm(),
                null,
                pendentes);
    }
}
