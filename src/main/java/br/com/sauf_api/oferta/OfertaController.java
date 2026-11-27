
package br.com.sauf_api.oferta;

import br.com.sauf_api.comum.Pagina;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1/ofertas")
public class OfertaController {

    private final OfertaService service;

    public OfertaController(OfertaService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<OfertaResposta> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) UUID area,
            @RequestParam(required = false) Modalidade modalidade,
            @RequestParam(required = false) Grau grau,
            @RequestParam(required = false) Integer duracaoSemestres,
            @RequestParam(required = false) BigDecimal notaCorteMax,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String cidade,
            @PageableDefault(
                    size = 12,
                    sort = "nomeNoSite",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        var filtro = new FiltroOfertas(
                modalidade,
                grau,
                duracaoSemestres,
                uf,
                cidade
        );

        return service.listar(
                filtro,
                busca,
                area,
                notaCorteMax,
                pageable
        );
    }
    @GetMapping("/{id}")
    public OfertaResposta detalhar(@PathVariable UUID id) {
        return service.detalhar(id);
    }
}
