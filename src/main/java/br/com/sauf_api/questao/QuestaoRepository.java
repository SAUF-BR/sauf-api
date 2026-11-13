package br.com.sauf_api.questao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestaoRepository extends JpaRepository<Questao, UUID> {

    // Sem filtro por língua: o null do Java é '' no banco e o "is null" do Spring Data não acharia.
    List<Questao> findByProva(Prova prova);

    // Língua diferente de ESPANHOL: só há inglês no banco e o simulado não mistura línguas.
    @Query("""
            select q.id from Questao q
            where q.prova.banca = :banca and q.area = :area and q.status = :status
            and q.anulada = false and q.lingua <> :linguaExcluida
            """)
    List<UUID> idsSorteaveis(Banca banca, AreaQuestao area, StatusQuestao status, Lingua linguaExcluida);

    @Query("""
            select count(q) from Questao q
            where q.prova.banca = :banca and q.area = :area and q.status = :status
            and q.anulada = false and q.lingua <> :linguaExcluida
            """)
    long contarSorteaveis(Banca banca, AreaQuestao area, StatusQuestao status, Lingua linguaExcluida);
}
