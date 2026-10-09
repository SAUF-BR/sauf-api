package br.com.sauf_api.curso;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaResposta;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService service;

    public CursoController(CursoService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<CursoResumo> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) UUID area,
            @RequestParam(required = false) Modalidade modalidade,
            @RequestParam(required = false) Grau grau,
            @RequestParam(required = false) Integer duracaoSemestres,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade,
            @PageableDefault(size = 12, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        var ofertas = new FiltroOfertas(modalidade, grau, duracaoSemestres, uf, cidade);
        return service.listar(new FiltroCursos(busca, area, ofertas), pageable);
    }

    @GetMapping("/{id}")
    public CursoResumo detalhar(
            @PathVariable UUID id,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade) {
        return service.detalhar(id, new FiltroOfertas(null, null, null, uf, cidade));
    }

    @GetMapping("/{id}/ofertas")
    public Pagina<OfertaResposta> ofertas(
            @PathVariable UUID id,
            @RequestParam(required = false) Modalidade modalidade,
            @RequestParam(required = false) Grau grau,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade,
            @PageableDefault(size = 20, sort = "nomeNoSite", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.ofertas(id, new FiltroOfertas(modalidade, grau, null, uf, cidade), pageable);
    }
}
