package br.com.sauf_api.pagamento;

import br.com.sauf_api.comum.UsuarioLogado;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    private final PortalService service;
    private final UsuarioLogado usuarioLogado;

    public PortalController(PortalService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @PostMapping("/api/v1/assinatura/portal")
    public PortalResposta criar() {
        return new PortalResposta(service.criarSessao(usuarioLogado.id()));
    }
}
