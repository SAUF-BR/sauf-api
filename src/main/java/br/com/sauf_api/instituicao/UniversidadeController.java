package br.com.sauf_api.instituicao;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.Grau;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaResposta;
import java.util.List;
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
@RequestMapping("/api/v1/universidades")
public class UniversidadeController {

    private final UniversidadeService service;

    public UniversidadeController(UniversidadeService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<UniversidadeResumo> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) List<TipoInstituicao> tipo,
            @RequestParam(required = false) List<Modalidade> modalidade,
            @RequestParam(required = false) FaixaNotaMec notaMec,
            @PageableDefault(size = 12, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(new FiltroUniversidades(busca, uf, cidade, tipo, modalidade, notaMec), pageable);
    }

    @GetMapping("/contagens")
    public ContagensUniversidades contagens(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) List<TipoInstituicao> tipo,
            @RequestParam(required = false) List<Modalidade> modalidade,
            @RequestParam(required = false) FaixaNotaMec notaMec) {
        return service.contagens(new FiltroUniversidades(busca, uf, cidade, tipo, modalidade, notaMec));
    }

    @GetMapping("/cidades")
    public List<String> cidades(@RequestParam(required = false) String uf) {
        return service.cidades(uf);
    }

    @GetMapping("/{id}")
    public UniversidadeDetalhe detalhar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    @GetMapping("/{id}/ofertas")
    public Pagina<OfertaResposta> ofertas(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID area,
            @RequestParam(required = false) Modalidade modalidade,
            @RequestParam(required = false) Grau grau,
            @PageableDefault(size = 20, sort = "nomeNoSite", direction = Sort.Direction.ASC) Pageable pageable) {
        var filtro = new FiltroOfertas(modalidade, grau, null, null, null);
        return service.ofertas(id, filtro, area, pageable);
    }
}