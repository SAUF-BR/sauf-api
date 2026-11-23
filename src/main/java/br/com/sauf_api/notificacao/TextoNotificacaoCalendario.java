package br.com.sauf_api.notificacao;

import br.com.sauf_api.calendario.EventoCalendario;
import br.com.sauf_api.calendario.TipoEvento;

/**
 * Monta título e descrição das notificações geradas a partir de eventos do calendário.
 * Fica fora do serviço para facilitar o teste e para o front não depender do formato.
 */
final class TextoNotificacaoCalendario {

    static final String ROTULO_ACAO = "Ver calendário";
    static final String ROTA_ACAO = "/calendario";

    private TextoNotificacaoCalendario() {}

    static String titulo(EventoCalendario evento) {
        return rotuloTipo(evento.getTipo()) + ": " + evento.getTitulo();
    }

    static String descricao(JanelaAviso janela) {
        return switch (janela) {
            case SETE_DIAS -> "Faltam 7 dias";
            case TRES_DIAS -> "Faltam 3 dias";
            case UM_DIA -> "Amanhã";
            case NO_DIA -> "É hoje";
        };
    }

    private static String rotuloTipo(TipoEvento tipo) {
        return switch (tipo) {
            case INSCRICAO -> "Inscrição";
            case PROVA -> "Prova";
            case RESULTADO -> "Resultado";
            case FEIRA -> "Feira";
            case OUTRO -> "Evento";
        };
    }
}
