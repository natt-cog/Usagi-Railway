package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.Formation;

public interface FormationRepository extends JpaRepository<Formation, String> {

    List<Formation> findAllByOrderByFormationNoAsc();
}
