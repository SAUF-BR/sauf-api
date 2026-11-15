package br.com.sauf_api.auth;

import br.com.sauf_api.comum.Sessao;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Gera e confere o token da sessão (JWT HS256, D-84). O id do usuário vai no "sub". */
@Service
public class TokenService {

    private static final String EMISSOR = "sauf-api";
    private static final String CLAIM_TROCA_SENHA = "troca_senha_ate";
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration duracao;

    public TokenService(
            @Value("${sauf.auth.jwt-secret:}") String segredoBase64,
            @Value("${sauf.auth.sessao-dias:7}") int sessaoDias) {
        SecretKey chave = chave(segredoBase64);
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        this.duracao = Duration.ofDays(sessaoDias);
    }

    public String gerar(UUID usuarioId) {
        return gerar(usuarioId, null);
    }

    /**
     * Sessão de quem entrou pelo código do "esqueci minha senha" (D-90): leva até quando a
     * pessoa pode criar uma senha nova sem informar a atual.
     */
    public String gerar(UUID usuarioId, Instant trocaSenhaSemAtualAte) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(duracao));
        if (trocaSenhaSemAtualAte != null) {
            claims.claim(CLAIM_TROCA_SENHA, trocaSenhaSemAtualAte.getEpochSecond());
        }
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
    }

    /** A sessão se o token for válido (assinatura e validade); vazio caso contrário. */
    public Optional<Sessao> validar(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            Object troca = jwt.getClaims().get(CLAIM_TROCA_SENHA);
            Instant trocaAte = troca instanceof Number segundos ? Instant.ofEpochSecond(segundos.longValue()) : null;
            return Optional.of(new Sessao(UUID.fromString(jwt.getSubject()), trocaAte));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public Duration duracao() {
        return duracao;
    }

    private static SecretKey chave(String segredoBase64) {
        if (segredoBase64 == null || segredoBase64.isBlank()) {
            throw new IllegalStateException(
                    "SAUF_JWT_SECRET não está configurado. Gere um segredo (32 bytes em base64) e coloque no .env (veja o README).");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(segredoBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("SAUF_JWT_SECRET deve estar em base64.", e);
        }
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("SAUF_JWT_SECRET deve ter pelo menos 32 bytes (em base64).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
