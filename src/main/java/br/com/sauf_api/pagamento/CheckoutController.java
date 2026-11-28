package br.com.sauf_api.pagamento;

import br.com.sauf_api.comum.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService service;
    private final UsuarioLogado usuarioLogado;

    public CheckoutController(CheckoutService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @PostMapping("/api/v1/assinatura/checkout")
    public CheckoutResposta criar(@Valid @RequestBody CheckoutRequest request) {
        return new CheckoutResposta(service.criarSessao(usuarioLogado.id(), request.plano()));
    }
}
