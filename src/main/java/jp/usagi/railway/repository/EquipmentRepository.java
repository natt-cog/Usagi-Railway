package jp.usagi.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.usagi.railway.domain.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, String> {

    List<Equipment> findByCarFormationFormationNoOrderByCarPositionAscEquipmentTypeAsc(String formationNo);

    List<Equipment> findByCarIsNullOrderBySerialNoAsc();
}
