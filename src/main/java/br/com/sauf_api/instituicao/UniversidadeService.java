package br.com.sauf_api.instituicao;

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
public class UniversidadeService {

    private static final EstatisticasOfertas.PorInstituicao SEM_OFERTAS = new EstatisticasOfertas.PorInstituicao(0, Set.of());

    private final InstituicaoRepository instituicoes;
    private final OfertaCursoRepository ofertas;
    private final EstatisticasOfertas estatisticas;
    private final NotasSisu sisu;

    public UniversidadeService(
            InstituicaoRepository instituicoes,
            OfertaCursoRepository ofertas,
            EstatisticasOfertas estatisticas,
            NotasSisu sisu) {
        this.instituicoes = instituicoes;
        this.ofertas = ofertas;
        this.estatisticas = estatisticas;
        this.sisu = sisu;
    }

    public Pagina<UniversidadeResumo> listar(FiltroUniversidades filtro, Pageable pageable) {
        var pagina = instituicoes.findAll(filtro.especificacao(), pageable);
        var numeros = estatisticas.porInstituicao(pagina.map(Instituicao::getId).getContent());
        return Pagina.de(pagina, i -> UniversidadeResumo.de(i, numeros.getOrDefault(i.getId(), SEM_OFERTAS)));
    }

    public UniversidadeDetalhe detalhar(UUID id) {
        Instituicao i = buscar(id);
        var numeros = estatisticas.porInstituicao(Set.of(id)).getOrDefault(id, SEM_OFERTAS);
        var cursosPorArea = estatisticas.cursosPorArea(id).entrySet().stream()
                .map(e -> new UniversidadeDetalhe.AreaCursos(e.getKey(), e.getValue()))
                .toList();
        return new UniversidadeDetalhe(
                i.getId(),
                i.getCodigoEmec(),
                i.getNome(),
                i.getSigla(),
                i.getTipo(),
                i.getCategoriaAdministrativa(),
                i.getOrganizacaoAcademica(),
                i.getCidade(),
                i.getUf(),
                UniversidadeDetalhe.EnderecoResposta.de(i),
                i.getSiteUrl(),
                i.getLogoUrl(),
                i.getNotaMec(),
                numeros.totalCursos(),
                numeros.modalidades(),
                cursosPorArea,
                i.getAtualizadaEm());
    }

    public Pagina<OfertaResposta> ofertas(UUID id, FiltroOfertas filtro, UUID areaId, Pageable pageable) {
        buscar(id);
        var pagina = ofertas.findAll(OfertaEspecificacoes.daInstituicao(id, filtro, areaId), pageable);
        return OfertaResposta.pagina(pagina, sisu);
    }

    private Instituicao buscar(UUID id) {
        return instituicoes.findByIdAndAtivaTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Universidade", id));
    }
}
