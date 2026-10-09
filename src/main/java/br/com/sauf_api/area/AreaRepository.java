package br.com.sauf_api.area;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, UUID> {

    Optional<Area> findByCodigoCine(String codigoCine);
}
