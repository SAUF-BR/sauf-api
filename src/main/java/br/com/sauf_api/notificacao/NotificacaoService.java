package br.com.sauf_api.notificacao;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository notificacoes, Clock clock) {
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    public List<NotificacaoResposta> listar(UUID usuarioId) {
        return notificacoes.findByUsuarioIdOrderByCriadaEmDesc(usuarioId).stream()
                .map(NotificacaoResposta::de)
                .toList();
    }

    public long contarNaoLidas(UUID usuarioId) {
        return notificacoes.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarTodasComoLidas(UUID usuarioId) {
        notificacoes.marcarTodasComoLidas(usuarioId, clock.instant());
    }
}
