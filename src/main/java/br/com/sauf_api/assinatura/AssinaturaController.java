package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assinatura")
public class AssinaturaController {

    private final AssinaturaService service;
    private final UsuarioLogado usuarioLogado;

    public AssinaturaController(AssinaturaService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/planos")
    public List<PlanoResposta> planos() {
        return service.listarPlanos();
    }

    @GetMapping
    public ResponseEntity<AssinaturaResposta> atual() {
        return service.atual(usuarioLogado.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public AssinaturaResposta trocarPlano(@Valid @RequestBody TrocarPlanoRequest request) {
        return service.trocarPlano(usuarioLogado.id(), request.plano());
    }

    @PostMapping("/cancelar")
    public AssinaturaResposta cancelar() {
        return service.cancelar(usuarioLogado.id());
    }

    @PostMapping("/reativar")
    public AssinaturaResposta reativar() {
        return service.reativar(usuarioLogado.id());
    }
}
