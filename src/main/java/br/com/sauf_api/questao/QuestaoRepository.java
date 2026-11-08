package br.com.sauf_api.questao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestaoRepository extends JpaRepository<Questao, UUID> {

    // Sem filtro por língua: o null do Java é '' no banco e o "is null" do Spring Data não acharia.
    List<Questao> findByProva(Prova prova);
}
