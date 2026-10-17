package br.com.sauf_api.curso;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.OfertaEspecificacoes;
import br.com.sauf_api.oferta.OfertaResposta;
import br.com.sauf_api.sisu.NotasSisu;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CursoService {

    private static final EstatisticasOfertas.PorCurso SEM_OFERTAS =
            new EstatisticasOfertas.PorCurso(0, Set.of(), Set.of(), null, null);

    private final CursoRepository cursos;
    private final OfertaCursoRepository ofertas;
    private final EstatisticasOfertas estatisticas;
    private final NotasSisu sisu;

    public CursoService(
            CursoRepository cursos, OfertaCursoRepository ofertas, EstatisticasOfertas estatisticas, NotasSisu sisu) {
        this.cursos = cursos;
        this.ofertas = ofertas;
        this.estatisticas = estatisticas;
        this.sisu = sisu;
    }

    public Pagina<CursoResumo> listar(FiltroCursos filtro, Pageable pageable) {
        var pagina = cursos.findAll(filtro.especificacao(), pageable);
        var ids = pagina.map(Curso::getId).getContent();
        var numeros = estatisticas.porCurso(ids, filtro.ofertas());
        var medias = sisu.media(estatisticas.codigosEmecPorCurso(ids, filtro.ofertas()));
        return Pagina.de(pagina, c -> CursoResumo.de(
                c, numeros.getOrDefault(c.getId(), SEM_OFERTAS), medias.get(c.getId())));
    }

    public CursoResumo detalhar(UUID id, FiltroOfertas filtro) {
        Curso curso = buscar(id);
        var numeros = estatisticas.porCurso(Set.of(id), filtro).getOrDefault(id, SEM_OFERTAS);
        var media = sisu.media(estatisticas.codigosEmecPorCurso(Set.of(id), filtro)).get(id);
        return CursoResumo.de(curso, numeros, media);
    }

    public Pagina<OfertaResposta> ofertas(UUID id, FiltroOfertas filtro, Pageable pageable) {
        buscar(id);
        var pagina = ofertas.findAll(OfertaEspecificacoes.doCurso(id, filtro), pageable);
        return OfertaResposta.pagina(pagina, sisu);
    }

    private Curso buscar(UUID id) {
        return cursos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Curso", id));
    }
}
