package br.com.sauf_api.oferta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EstatisticasOfertas {

    private final EntityManager em;

    public EstatisticasOfertas(EntityManager em) {
        this.em = em;
    }

    public record PorCurso(
            long totalUniversidades,
            Set<Modalidade> modalidades,
            Set<Grau> graus,
            Integer duracaoSemestresMin,
            Integer duracaoSemestresMax,
            BigDecimal notaCorteSisuMin) {}

    public record PorInstituicao(long totalCursos, Set<Modalidade> modalidades) {}

    public Map<UUID, PorCurso> porCurso(Collection<UUID> cursoIds, FiltroOfertas filtro) {
        if (cursoIds.isEmpty()) {
            return Map.of();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<UUID> cursoId = oferta.get("curso").get("id");
        Path<Integer> duracao = oferta.get("duracaoSemestres");
        Path<BigDecimal> nota = oferta.get("notaCorteSisu");
        q.multiselect(
                        cursoId,
                        cb.countDistinct(oferta.get("instituicao").get("id")),
                        cb.min(duracao),
                        cb.max(duracao),
                        cb.min(nota))
                .where(restricoes(cb, oferta, filtro, cursoId, cursoIds))
                .groupBy(cursoId);

        Map<UUID, Set<Modalidade>> modalidades = modalidades(filtro, "curso", cursoIds);
        Map<UUID, Set<Grau>> graus = grausPorCurso(filtro, cursoIds);
        Map<UUID, PorCurso> resultado = new HashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            UUID id = t.get(0, UUID.class);
            resultado.put(id, new PorCurso(
                    t.get(1, Long.class),
                    modalidades.getOrDefault(id, Set.of()),
                    graus.getOrDefault(id, Set.of()),
                    t.get(2, Integer.class),
                    t.get(3, Integer.class),
                    t.get(4, BigDecimal.class)));
        }
        return resultado;
    }

    public Map<UUID, PorInstituicao> porInstituicao(Collection<UUID> instituicaoIds) {
        if (instituicaoIds.isEmpty()) {
            return Map.of();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<UUID> instituicaoId = oferta.get("instituicao").get("id");
        q.multiselect(instituicaoId, cb.count(oferta))
                .where(restricoes(cb, oferta, FiltroOfertas.vazio(), instituicaoId, instituicaoIds))
                .groupBy(instituicaoId);

        Map<UUID, Set<Modalidade>> modalidades = modalidades(FiltroOfertas.vazio(), "instituicao", instituicaoIds);
        Map<UUID, PorInstituicao> resultado = new HashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            UUID id = t.get(0, UUID.class);
            resultado.put(id, new PorInstituicao(t.get(1, Long.class), modalidades.getOrDefault(id, Set.of())));
        }
        return resultado;
    }

    public Map<String, Long> cursosPorArea(UUID instituicaoId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<String> area = oferta.get("curso").get("area").get("nome");
        q.multiselect(area, cb.count(oferta))
                .where(FiltroOfertas.visivel(oferta, cb), cb.equal(oferta.get("instituicao").get("id"), instituicaoId))
                .groupBy(area)
                .orderBy(cb.desc(cb.count(oferta)));

        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            resultado.put(t.get(0, String.class), t.get(1, Long.class));
        }
        return resultado;
    }

    public Map<UUID, Long> cursosPorAreaId() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<UUID> areaId = oferta.get("curso").get("area").get("id");
        q.multiselect(areaId, cb.countDistinct(oferta.get("curso").get("id")))
                .where(FiltroOfertas.visivel(oferta, cb))
                .groupBy(areaId);

        Map<UUID, Long> resultado = new HashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            resultado.put(t.get(0, UUID.class), t.get(1, Long.class));
        }
        return resultado;
    }

    private Map<UUID, Set<Grau>> grausPorCurso(FiltroOfertas filtro, Collection<UUID> cursoIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<UUID> cursoId = oferta.get("curso").get("id");
        Path<Grau> grau = oferta.join("opcoes").get("grau");
        q.multiselect(cursoId, grau).distinct(true).where(restricoes(cb, oferta, filtro, cursoId, cursoIds));

        Map<UUID, Set<Grau>> resultado = new HashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            resultado.computeIfAbsent(t.get(0, UUID.class), k -> EnumSet.noneOf(Grau.class))
                    .add(t.get(1, Grau.class));
        }
        return resultado;
    }

    private Map<UUID, Set<Modalidade>> modalidades(FiltroOfertas filtro, String dono, Collection<UUID> ids) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<OfertaCurso> oferta = q.from(OfertaCurso.class);
        Path<UUID> donoId = oferta.get(dono).get("id");
        Path<Modalidade> modalidade = oferta.get("modalidade");
        Predicate[] restricoes = restricoes(cb, oferta, filtro, donoId, ids);
        q.multiselect(donoId, modalidade)
                .distinct(true)
                .where(cb.and(restricoes), cb.isNotNull(modalidade));

        Map<UUID, Set<Modalidade>> resultado = new HashMap<>();
        for (Tuple t : em.createQuery(q).getResultList()) {
            resultado.computeIfAbsent(t.get(0, UUID.class), k -> EnumSet.noneOf(Modalidade.class))
                    .add(t.get(1, Modalidade.class));
        }
        return resultado;
    }

    private Predicate[] restricoes(
            CriteriaBuilder cb, Root<OfertaCurso> oferta, FiltroOfertas filtro, Path<UUID> donoId, Collection<UUID> ids) {
        List<Predicate> predicados = filtro.predicados(oferta, cb);
        predicados.add(donoId.in(ids));
        return predicados.toArray(Predicate[]::new);
    }
}
