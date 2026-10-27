package com.mediconnect.hospital;

import com.mediconnect.audit.AuditService;
import com.mediconnect.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitals;
    private final BloodStockRepository bloodStock;
    private final OrganAvailabilityRepository organs;
    private final AuditService audit;

    public record HospitalDto(Long id, String name, String city, String address, String phone) {
        static HospitalDto from(Hospital h) {
            return new HospitalDto(h.getId(), h.getName(), h.getCity(), h.getAddress(), h.getPhone());
        }
    }

    public record StockDto(Long id, Long hospitalId, String hospitalName, String city, String phone,
                           String item, int quantity, LocalDateTime updatedAt) {
    }

    @Transactional(readOnly = true)
    public List<HospitalDto> all() {
        return hospitals.findAllByOrderByCityAscNameAsc().stream().map(HospitalDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return hospitals.findCities();
    }

    @Transactional(readOnly = true)
    public List<String> organTypes() {
        return organs.findOrgans();
    }

    @Transactional(readOnly = true)
    public List<StockDto> blood(String group, String city) {
        return bloodStock.search(blank(group), blank(city)).stream()
                .map(b -> new StockDto(b.getId(), b.getHospital().getId(), b.getHospital().getName(),
                        b.getHospital().getCity(), b.getHospital().getPhone(), b.getBloodGroup(), b.getUnits(),
                        b.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockDto> organs(String organ, String city) {
        return organs.search(blank(organ), blank(city)).stream()
                .map(o -> new StockDto(o.getId(), o.getHospital().getId(), o.getHospital().getName(),
                        o.getHospital().getCity(), o.getHospital().getPhone(), o.getOrgan(), o.getAvailable(),
                        o.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public void updateBlood(Long id, int units) {
        BloodStock b = bloodStock.findById(id).orElseThrow(() -> ApiException.notFound("Blood stock"));
        audit.record("UPDATE_BLOOD_STOCK", "BloodStock", id,
                b.getBloodGroup() + ": " + b.getUnits() + " -> " + units);
        b.setUnits(units);
    }

    @Transactional
    public void updateOrgan(Long id, int available) {
        OrganAvailability o = organs.findById(id).orElseThrow(() -> ApiException.notFound("Organ record"));
        audit.record("UPDATE_ORGAN_STOCK", "OrganAvailability", id,
                o.getOrgan() + ": " + o.getAvailable() + " -> " + available);
        o.setAvailable(available);
    }

    @Transactional(readOnly = true)
    public Hospital getEntity(Long id) {
        return hospitals.findById(id).orElseThrow(() -> ApiException.notFound("Hospital"));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
