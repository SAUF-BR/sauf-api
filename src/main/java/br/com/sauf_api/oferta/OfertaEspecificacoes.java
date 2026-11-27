package br.com.sauf_api.oferta;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class OfertaEspecificacoes {

    private OfertaEspecificacoes() {}

    public static Specification<OfertaCurso> todas(
            FiltroOfertas filtro,
            String busca,
            UUID areaId) {

        return (oferta, query, cb) -> {
            List<Predicate> predicados = filtro.predicados(oferta, cb);

            if (busca != null && !busca.isBlank()) {
                predicados.add(cb.like(
                        cb.lower(oferta.get("curso").get("nome")),
                        "%" + busca.trim().toLowerCase() + "%"
                ));
            }

            if (areaId != null) {
                predicados.add(cb.equal(
                        oferta.get("curso").get("area").get("id"),
                        areaId
                ));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    public static Specification<OfertaCurso> doCurso(
            UUID cursoId,
            FiltroOfertas filtro) {

        return (oferta, query, cb) -> {
            List<Predicate> predicados = filtro.predicados(oferta, cb);
            predicados.add(cb.equal(oferta.get("curso").get("id"), cursoId));

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    public static Specification<OfertaCurso> daInstituicao(
            UUID instituicaoId,
            FiltroOfertas filtro,
            UUID areaId) {

        return (oferta, query, cb) -> {
            List<Predicate> predicados = filtro.predicados(oferta, cb);
            predicados.add(cb.equal(
                    oferta.get("instituicao").get("id"),
                    instituicaoId
            ));

            if (areaId != null) {
                predicados.add(cb.equal(
                        oferta.get("curso").get("area").get("id"),
                        areaId
                ));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
