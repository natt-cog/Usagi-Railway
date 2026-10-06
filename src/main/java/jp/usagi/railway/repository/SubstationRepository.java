package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.Substation;

public interface SubstationRepository extends JpaRepository<Substation, String> {

    List<Substation> findAllByOrderByCodeAsc();
}
