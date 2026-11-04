package br.com.sauf_api.instituicao;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.curso.Curso;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.OfertaEspecificacoes;
import br.com.sauf_api.oferta.OfertaResposta;
import br.com.sauf_api.sisu.NotasSisu;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    public ContagensUniversidades contagens(FiltroUniversidades filtro) {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (TipoInstituicao tipo : TipoInstituicao.values()) {
            porTipo.put(tipo.valor(), instituicoes.count(filtro.comTipos(List.of(tipo)).especificacao()));
        }
        Map<String, Long> porModalidade = new LinkedHashMap<>();
        for (Modalidade modalidade : Modalidade.values()) {
            porModalidade.put(modalidade.valor(),
                    instituicoes.count(filtro.comModalidades(List.of(modalidade)).especificacao()));
        }
        return new ContagensUniversidades(porTipo, porModalidade);
    }

    public List<String> cidades(String uf) {
        if (uf == null || uf.isBlank()) {
            return List.of();
        }
        return instituicoes.cidadesComUniversidadeAtiva(uf.trim().toUpperCase());
    }

    public UniversidadeDetalhe detalhar(UUID id) {
        Instituicao i = buscar(id);
        var numeros = estatisticas.porInstituicao(Set.of(id)).getOrDefault(id, SEM_OFERTAS);
        var cursosPorArea = estatisticas.cursosPorArea(id).entrySet().stream()
                .map(e -> new UniversidadeDetalhe.AreaCursos(e.getKey(), e.getValue()))
                .toList();
        List<OfertaCurso> ofertasAtivas = ofertas.findByInstituicaoIdAndAtivaTrue(id);
        return new UniversidadeDetalhe(
                i.getId(),
                i.getCodigoEmec(),
                i.getNome(),
                i.getSigla(),
                i.getTipo(),
                esfera(i.getCategoriaAdministrativa()),
                i.getCategoriaAdministrativa(),
                i.getOrganizacaoAcademica(),
                i.getCidade(),
                i.getUf(),
                UniversidadeDetalhe.EnderecoResposta.de(i),
                i.getSiteUrl(),
                i.getLogoUrl(),
                null, // capaUrl: ainda sem fonte
                null, // anoFundacao: ainda sem fonte
                i.getNotaMec(),
                gratuita(ofertasAtivas),
                vagasPorAno(ofertasAtivas),
                numeros.totalCursos(),
                List.of(), // sobre: ainda sem fonte
                modalidadesOfertadas(ofertasAtivas),
                cursosPorArea,
                List.of(), // formasIngresso: ainda sem fonte
                null, // notaFormasIngresso: ainda sem fonte
                null, // proximoPrazo: ainda sem fonte
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

    static Esfera esfera(String categoriaAdministrativa) {
        if (categoriaAdministrativa == null) {
            return null;
        }
        String categoria = FiltroUniversidades.normalizar(categoriaAdministrativa.replace('_', ' '));
        if (!categoria.startsWith("publica")) {
            return null;
        }
        for (Esfera esfera : Esfera.values()) {
            if (categoria.contains(esfera.valor())) {
                return esfera;
            }
        }
        return null;
    }

    static boolean gratuita(List<OfertaCurso> ofertas) {
        List<Boolean> informadas = ofertas.stream()
                .map(OfertaCurso::getGratuito)
                .filter(Objects::nonNull)
                .toList();
        return !informadas.isEmpty() && informadas.stream().allMatch(Boolean::booleanValue);
    }

    static Integer vagasPorAno(List<OfertaCurso> ofertas) {
        return ofertas.stream()
                .map(OfertaCurso::getVagas)
                .filter(Objects::nonNull)
                .reduce(Integer::sum)
                .orElse(null);
    }

    static List<UniversidadeDetalhe.ModalidadeOfertada> modalidadesOfertadas(List<OfertaCurso> ofertas) {
        Map<Modalidade, Set<Curso>> cursosPorModalidade = ofertas.stream()
                .filter(o -> o.getModalidade() != null)
                .collect(Collectors.groupingBy(
                        OfertaCurso::getModalidade, () -> new EnumMap<>(Modalidade.class),
                        Collectors.mapping(OfertaCurso::getCurso, Collectors.toSet())));
        return Arrays.stream(Modalidade.values())
                .map(m -> {
                    long total = cursosPorModalidade.getOrDefault(m, Set.of()).size();
                    return new UniversidadeDetalhe.ModalidadeOfertada(m, total > 0, total, null);
                })
                .toList();
    }
}