package br.com.sauf_api.notificacao;

import br.com.sauf_api.comum.UsuarioLogado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;
    private final UsuarioLogado usuarioLogado;

    public NotificacaoController(NotificacaoService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping
    public List<NotificacaoResposta> listar() {
        return service.listar(usuarioLogado.id());
    }

    @GetMapping("/nao-lidas")
    public TotalNaoLidas contarNaoLidas() {
        return new TotalNaoLidas(service.contarNaoLidas(usuarioLogado.id()));
    }

    @PatchMapping("/marcar-todas-como-lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasComoLidas() {
        service.marcarTodasComoLidas(usuarioLogado.id());
    }
}
