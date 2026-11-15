package br.com.sauf_api.auth;

import br.com.sauf_api.comum.CampoInvalidoException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Código de 6 dígitos enviado por e-mail no "esqueci minha senha" (D-90). Vale 10 minutos,
 * uma vez, com no máximo 5 tentativas; o banco guarda só o hash BCrypt. Pedir outro
 * invalida o anterior.
 */
@Service
public class CodigoAcessoService {

    static final String MENSAGEM_INVALIDO = "Código inválido ou vencido. Peça um novo código.";
    // Intervalo mínimo entre dois e-mails para a mesma conta
    static final Duration INTERVALO_REENVIO = Duration.ofMinutes(1);

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TokenEmailRepository codigos;
    private final PasswordEncoder hashes;

    public CodigoAcessoService(TokenEmailRepository codigos, PasswordEncoder hashes) {
        this.codigos = codigos;
        this.hashes = hashes;
    }

    /** Novo código (invalida os anteriores). Devolve o código que vai no e-mail. */
    @Transactional
    public String criar(UUID usuarioId, TipoTokenEmail tipo) {
        Instant agora = Instant.now();
        codigos.invalidarPendentes(usuarioId, tipo, agora);
        String codigo = "%06d".formatted(ALEATORIO.nextInt(1_000_000));
        codigos.saveAndFlush(new TokenEmail(usuarioId, tipo, hashes.encode(codigo), agora));
        return codigo;
    }

    /** Se um código deste tipo foi pedido há pouco (para não mandar vários e-mails seguidos). */
    @Transactional(readOnly = true)
    public boolean pedidoRecente(UUID usuarioId, TipoTokenEmail tipo) {
        return codigos.ultimoCriadoEm(usuarioId, tipo)
                .map(ultimo -> ultimo.isAfter(Instant.now().minus(INTERVALO_REENVIO)))
                .orElse(false);
    }

    /**
     * Confere o código da pessoa e o marca como usado. Errado: 400 com quantas tentativas
     * restam. Sem código válido (vencido, usado ou 5 erros): 400 pedindo um novo.
     * A tentativa errada é gravada mesmo com o erro (noRollbackFor).
     */
    @Transactional(noRollbackFor = CampoInvalidoException.class)
    public void conferir(UUID usuarioId, TipoTokenEmail tipo, String codigo) {
        Instant agora = Instant.now();
        TokenEmail atual = codigos.findFirstByUsuarioIdAndTipoAndUsadoEmIsNullOrderByCriadoEmDesc(usuarioId, tipo)
                .filter(t -> t.vale(agora))
                .orElseThrow(() -> new CampoInvalidoException("codigo", MENSAGEM_INVALIDO));
        if (!hashes.matches(codigo.trim(), atual.getHash())) {
            int restantes = atual.errar();
            throw new CampoInvalidoException("codigo", restantes == 0
                    ? "Código incorreto. Você usou as 5 tentativas: peça um novo código."
                    : "Código incorreto. Você ainda tem " + restantes + (restantes == 1 ? " tentativa." : " tentativas."));
        }
        atual.usar(agora);
    }
}
