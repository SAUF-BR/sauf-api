package br.com.sauf_api.curso;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.imagem.ImagensCatalogo;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.OfertaEspecificacoes;
import br.com.sauf_api.oferta.OfertaResposta;
import br.com.sauf_api.sisu.NotasSisu;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
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
    private final ImagensCatalogo imagens;

    public CursoService(
            CursoRepository cursos,
            OfertaCursoRepository ofertas,
            EstatisticasOfertas estatisticas,
            NotasSisu sisu,
            ImagensCatalogo imagens) {
        this.cursos = cursos;
        this.ofertas = ofertas;
        this.estatisticas = estatisticas;
        this.sisu = sisu;
        this.imagens = imagens;
    }

    public Pagina<CursoResumo> listar(FiltroCursos filtro, Pageable pageable) {
        var imagensAreas = imagens.urlsAreas();

        // Sem filtro SISU, mantém a consulta paginada original.
        if (filtro.notaCorteMax() == null) {
            var pagina = cursos.findAll(filtro.especificacao(), pageable);
            var ids = pagina.map(Curso::getId).getContent();
            var numeros = estatisticas.porCurso(ids, filtro.ofertas());
            var medias = sisu.media(estatisticas.codigosEmecPorCurso(ids, filtro.ofertas()));
            return Pagina.de(pagina, c -> CursoResumo.de(
                    c, numeros.getOrDefault(c.getId(), SEM_OFERTAS), medias.get(c.getId()), imagemDaArea(c, imagensAreas)));
        }

        // Busca todos os cursos que atendem aos demais filtros.
        List<Curso> candidatos = cursos.findAll(filtro.especificacao(), pageable.getSort());

        // Calcula as médias SISU dos candidatos.
        List<UUID> idsCandidatos = candidatos.stream().map(Curso::getId).toList();
        Map<UUID, NotasSisu.Media> medias =
                sisu.media(estatisticas.codigosEmecPorCurso(idsCandidatos, filtro.ofertas()));

        // Mantém apenas cursos com média SISU dentro do limite.
        List<Curso> filtrados = candidatos.stream()
                .filter(c -> {
                    NotasSisu.Media media = medias.get(c.getId());
                    return media != null && media.nota().compareTo(filtro.notaCorteMax()) <= 0;
                })
                .toList();

        // Aplica a paginação somente depois de filtrar.
        int inicio = (int) Math.min(pageable.getOffset(), filtrados.size());
        int fim = Math.min(inicio + pageable.getPageSize(), filtrados.size());
        List<Curso> conteudo = filtrados.subList(inicio, fim);
        var pagina = new PageImpl<>(conteudo, pageable, filtrados.size());

        List<UUID> idsPagina = conteudo.stream().map(Curso::getId).toList();
        var numeros = estatisticas.porCurso(idsPagina, filtro.ofertas());

        return Pagina.de(pagina, c -> CursoResumo.de(
                c, numeros.getOrDefault(c.getId(), SEM_OFERTAS), medias.get(c.getId()), imagemDaArea(c, imagensAreas)));
    }

    public CursoResumo detalhar(UUID id, FiltroOfertas filtro) {
        Curso curso = buscar(id);
        var numeros = estatisticas.porCurso(Set.of(id), filtro).getOrDefault(id, SEM_OFERTAS);
        var media = sisu.media(estatisticas.codigosEmecPorCurso(Set.of(id), filtro)).get(id);
        return CursoResumo.de(curso, numeros, media, imagemDaArea(curso, imagens.urlsAreas()));
    }

    public Pagina<OfertaResposta> ofertas(UUID id, FiltroOfertas filtro, Pageable pageable) {
        buscar(id);

        var pagina = ofertas.findAll(
                OfertaEspecificacoes.doCurso(id, filtro),
                pageable
        );

        return OfertaResposta.pagina(pagina, sisu);
    }

    private static String imagemDaArea(Curso curso, Map<UUID, String> imagensAreas) {
        return curso.getArea() == null ? null : imagensAreas.get(curso.getArea().getId());
    }

    private Curso buscar(UUID id) {
        return cursos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso", id));
    }
}
