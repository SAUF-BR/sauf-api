package br.com.sauf_api.area;

import br.com.sauf_api.oferta.EstatisticasOfertas;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areas;
    private final EstatisticasOfertas estatisticas;

    public AreaService(AreaRepository areas, EstatisticasOfertas estatisticas) {
        this.areas = areas;
        this.estatisticas = estatisticas;
    }

    public List<AreaResposta> listar() {
        var cursosPorArea = estatisticas.cursosPorAreaId();
        return areas.findAll(Sort.by("codigoCine")).stream()
                .map(a -> new AreaResposta(a.getId(), a.getCodigoCine(), a.getNome(), cursosPorArea.getOrDefault(a.getId(), 0L)))
                .toList();
    }
}
