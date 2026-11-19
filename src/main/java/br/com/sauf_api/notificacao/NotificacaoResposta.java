package br.com.sauf_api.notificacao;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record NotificacaoResposta(
        UUID id,
        String titulo,
        String descricao,
        OffsetDateTime criadaEm,
        boolean lida,
        AcaoNotificacao acao) {

    public static NotificacaoResposta de(Notificacao n) {
        AcaoNotificacao acao = n.getAcaoRotulo() == null
                ? null
                : new AcaoNotificacao(n.getAcaoRotulo(), n.getAcaoPara());
        return new NotificacaoResposta(
                n.getId(),
                n.getTitulo(),
                n.getDescricao(),
                n.getCriadaEm().atOffset(ZoneOffset.UTC),
                n.isLida(),
                acao);
    }
}
