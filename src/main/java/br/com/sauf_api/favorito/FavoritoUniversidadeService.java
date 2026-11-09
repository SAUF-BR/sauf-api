package br.com.sauf_api.favorito;

import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.instituicao.Instituicao;
import br.com.sauf_api.instituicao.InstituicaoRepository;
import br.com.sauf_api.instituicao.UniversidadeResumo;
import br.com.sauf_api.oferta.EstatisticasOfertas;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FavoritoUniversidadeService {

    private static final EstatisticasOfertas.PorInstituicao SEM_OFERTAS = new EstatisticasOfertas.PorInstituicao(0, Set.of());

    private final FavoritoUniversidadeRepository favoritos;
    private final InstituicaoRepository instituicoes;
    private final EstatisticasOfertas estatisticas;

    public FavoritoUniversidadeService(
            FavoritoUniversidadeRepository favoritos,
            InstituicaoRepository instituicoes,
            EstatisticasOfertas estatisticas) {
        this.favoritos = favoritos;
        this.instituicoes = instituicoes;
        this.estatisticas = estatisticas;
    }

    public List<UniversidadeFavorita> listar(UUID usuarioId) {
        List<Instituicao> favoritas = favoritas(usuarioId);
        if (favoritas.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = favoritas.stream().map(Instituicao::getId).toList();
        var numeros = estatisticas.porInstituicao(ids);
        Map<UUID, Long> campi = campi(ids);
        return favoritas.stream()
                .map(i -> UniversidadeFavorita.de(
                        UniversidadeResumo.de(i, numeros.getOrDefault(i.getId(), SEM_OFERTAS)),
                        campi.getOrDefault(i.getId(), 0L)))
                .toList();
    }

    public List<UUID> ids(UUID usuarioId) {
        return favoritas(usuarioId).stream().map(Instituicao::getId).toList();
    }

    @Transactional
    public void favoritar(UUID usuarioId, UUID instituicaoId) {
        Instituicao instituicao = instituicoes.findByIdAndAtivaTrue(instituicaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Universidade", instituicaoId));
        if (!favoritos.existsByUsuarioIdAndInstituicaoId(usuarioId, instituicaoId)) {
            favoritos.saveAndFlush(new FavoritoUniversidade(usuarioId, instituicao));
        }
    }

    @Transactional
    public void desfavoritar(UUID usuarioId, UUID instituicaoId) {
        favoritos.deleteByUsuarioIdAndInstituicaoId(usuarioId, instituicaoId);
    }

    private List<Instituicao> favoritas(UUID usuarioId) {
        return favoritos.findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(usuarioId).stream()
                .map(FavoritoUniversidade::getInstituicao)
                .toList();
    }

    private Map<UUID, Long> campi(List<UUID> ids) {
        Map<UUID, Long> campi = new HashMap<>();
        for (Object[] linha : favoritos.campiPorInstituicao(ids)) {
            campi.put((UUID) linha[0], (Long) linha[1]);
        }
        return campi;
    }
}