package br.com.sauf_api.favorito;

import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.oferta.OfertaCurso;
import br.com.sauf_api.oferta.OfertaCursoRepository;
import br.com.sauf_api.oferta.OfertaResposta;
import br.com.sauf_api.sisu.NotasSisu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FavoritoCursoService {

    private final FavoritoCursoRepository favoritos;
    private final OfertaCursoRepository ofertas;
    private final NotasSisu sisu;

    public FavoritoCursoService(FavoritoCursoRepository favoritos, OfertaCursoRepository ofertas, NotasSisu sisu) {
        this.favoritos = favoritos;
        this.ofertas = ofertas;
        this.sisu = sisu;
    }

    public List<OfertaResposta> listar(UUID usuarioId) {
        List<OfertaCurso> favoritas = favoritas(usuarioId);
        if (favoritas.isEmpty()) {
            return List.of();
        }
        Map<UUID, Set<Integer>> codigos = new HashMap<>();
        for (OfertaCurso oferta : favoritas) {
            codigos.put(oferta.getId(), oferta.getCodigosEmec());
        }
        Map<UUID, NotasSisu.Corte> cortes = sisu.menorCorte(codigos);
        return favoritas.stream()
                .map(oferta -> OfertaResposta.de(oferta, cortes.get(oferta.getId())))
                .toList();
    }

    public List<UUID> ids(UUID usuarioId) {
        return favoritas(usuarioId).stream().map(OfertaCurso::getId).toList();
    }

    @Transactional
    public void favoritar(UUID usuarioId, UUID ofertaId) {
        OfertaCurso oferta = ofertas.findById(ofertaId)
                .filter(o -> o.isAtiva() && o.getInstituicao().isAtiva())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso", ofertaId));
        if (!favoritos.existsByUsuarioIdAndOfertaId(usuarioId, ofertaId)) {
            favoritos.save(new FavoritoCurso(usuarioId, oferta));
        }
    }

    @Transactional
    public void desfavoritar(UUID usuarioId, UUID ofertaId) {
        favoritos.deleteByUsuarioIdAndOfertaId(usuarioId, ofertaId);
    }

    private List<OfertaCurso> favoritas(UUID usuarioId) {
        return favoritos.findByUsuarioIdAndOfertaAtivaTrueAndOfertaInstituicaoAtivaTrueOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(FavoritoCurso::getOferta)
                .toList();
    }
}