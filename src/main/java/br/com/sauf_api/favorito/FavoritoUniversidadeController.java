package br.com.sauf_api.favorito;

import br.com.sauf_api.comum.UsuarioLogado;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favoritos/universidades")
public class FavoritoUniversidadeController {

    private final FavoritoUniversidadeService service;
    private final UsuarioLogado usuarioLogado;

    public FavoritoUniversidadeController(FavoritoUniversidadeService service, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping
    public List<UniversidadeFavorita> listar() {
        return service.listar(usuarioLogado.id());
    }

    @GetMapping("/ids")
    public List<UUID> ids() {
        return service.ids(usuarioLogado.id());
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favoritar(@PathVariable UUID id) {
        service.favoritar(usuarioLogado.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desfavoritar(@PathVariable UUID id) {
        service.desfavoritar(usuarioLogado.id(), id);
    }
}