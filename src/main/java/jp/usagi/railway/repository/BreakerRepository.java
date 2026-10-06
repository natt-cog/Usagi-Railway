package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.Breaker;

public interface BreakerRepository extends JpaRepository<Breaker, Long> {

    Breaker findBySubstationCodeAndBreakerCode(String substationCode, String breakerCode);

    List<Breaker> findAllByOrderBySubstationCodeAscBreakerCodeAsc();
}
