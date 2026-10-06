package jp.usagi.railway.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.usagi.railway.domain.Equipment;
import jp.usagi.railway.domain.Formation;
import jp.usagi.railway.repository.EquipmentRepository;
import jp.usagi.railway.repository.FormationRepository;

@Service
@Transactional(readOnly = true)
public class FormationService {

    private final FormationRepository formationRepository;
    private final EquipmentRepository equipmentRepository;

    public FormationService(FormationRepository formationRepository, EquipmentRepository equipmentRepository) {
        this.formationRepository = formationRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public List<Formation> list() {
        return formationRepository.findAllByOrderByFormationNoAsc();
    }

    public Formation get(String formationNo) {
        Formation f = formationRepository.findOne(formationNo);
        if (f == null) {
            throw new NotFoundException("編成", formationNo);
        }
        return f;
    }

    public List<Equipment> equipmentOf(String formationNo) {
        return equipmentRepository.findByCarFormationFormationNoOrderByCarPositionAscEquipmentTypeAsc(formationNo);
    }

    public List<Equipment> spares() {
        return equipmentRepository.findByCarIsNullOrderBySerialNoAsc();
    }

    public Equipment equipment(String serialNo) {
        Equipment e = equipmentRepository.findOne(serialNo);
        if (e == null) {
            throw new NotFoundException("機器 (製造番号)", serialNo);
        }
        return e;
    }
}
