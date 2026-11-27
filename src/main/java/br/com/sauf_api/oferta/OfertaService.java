
package br.com.sauf_api.oferta;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.sisu.NotasSisu;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;

@Service
@Transactional(readOnly = true)
public class OfertaService {

    private final OfertaCursoRepository ofertas;
    private final NotasSisu sisu;

    public OfertaService(OfertaCursoRepository ofertas, NotasSisu sisu) {
        this.ofertas = ofertas;
        this.sisu = sisu;
    }

    public Pagina<OfertaResposta> listar(
            FiltroOfertas filtro,
            String busca,
            UUID areaId,
            BigDecimal notaCorteMax,
            Pageable pageable) {

        var especificacao = OfertaEspecificacoes.todas(
                filtro,
                busca,
                areaId
        );

        // Sem filtro de nota: utiliza a paginação normal do banco.
        if (notaCorteMax == null) {
            var pagina = ofertas.findAll(especificacao, pageable);
            return OfertaResposta.pagina(pagina, sisu);
        }

        // Com filtro SISU: calcula as notas antes de paginar.
        List<OfertaCurso> candidatas = ofertas.findAll(
                especificacao,
                pageable.getSort()
        );

        List<OfertaResposta> filtradas = OfertaResposta
                .lista(candidatas, sisu)
                .stream()
                .filter(oferta -> oferta.notaCorteSisu() != null
                        && oferta.notaCorteSisu().compareTo(notaCorteMax) <= 0)
                .toList();

        int inicio = (int) Math.min(
                pageable.getOffset(),
                filtradas.size()
        );

        int fim = Math.min(
                inicio + pageable.getPageSize(),
                filtradas.size()
        );

        List<OfertaResposta> conteudo = filtradas.subList(inicio, fim);

        var pagina = new PageImpl<>(
                conteudo,
                pageable,
                filtradas.size()
        );

        return new Pagina<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages()
        );
    }

    public OfertaResposta detalhar(UUID id) {
        OfertaCurso oferta = ofertas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta", id));

        return OfertaResposta.de(
                oferta,
                sisu.menorCorte(
                        java.util.Map.of(oferta.getId(), oferta.getCodigosEmec())
                ).get(oferta.getId())
        );
    }
}
