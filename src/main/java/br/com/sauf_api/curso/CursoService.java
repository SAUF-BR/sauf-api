package br.com.sauf_api.curso;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.OfertaEspecificacoes;
import br.com.sauf_api.oferta.OfertaResposta;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CursoService {

    private static final EstatisticasOfertas.PorCurso SEM_OFERTAS =
            new EstatisticasOfertas.PorCurso(0, Set.of(), Set.of(), null, null, null);

    private final CursoRepository cursos;
    private final OfertaCursoRepository ofertas;
    private final EstatisticasOfertas estatisticas;

    public CursoService(CursoRepository cursos, OfertaCursoRepository ofertas, EstatisticasOfertas estatisticas) {
        this.cursos = cursos;
        this.ofertas = ofertas;
        this.estatisticas = estatisticas;
    }

    public Pagina<CursoResumo> listar(FiltroCursos filtro, Pageable pageable) {
        var pagina = cursos.findAll(filtro.especificacao(), pageable);
        var numeros = estatisticas.porCurso(pagina.map(Curso::getId).getContent(), filtro.ofertas());
        return Pagina.de(pagina, c -> CursoResumo.de(c, numeros.getOrDefault(c.getId(), SEM_OFERTAS)));
    }

    public CursoResumo detalhar(UUID id, FiltroOfertas filtro) {
        Curso curso = buscar(id);
        var numeros = estatisticas.porCurso(Set.of(id), filtro).getOrDefault(id, SEM_OFERTAS);
        return CursoResumo.de(curso, numeros);
    }

    public Pagina<OfertaResposta> ofertas(UUID id, FiltroOfertas filtro, Pageable pageable) {
        buscar(id);
        var pagina = ofertas.findAll(OfertaEspecificacoes.doCurso(id, filtro), pageable);
        return Pagina.de(pagina, OfertaResposta::de);
    }

    private Curso buscar(UUID id) {
        return cursos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Curso", id));
    }
}
