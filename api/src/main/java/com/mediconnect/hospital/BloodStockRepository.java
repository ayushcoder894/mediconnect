package com.mediconnect.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BloodStockRepository extends JpaRepository<BloodStock, Long> {

    @Query("""
            select b from BloodStock b join fetch b.hospital h
            where (:group is null or b.bloodGroup = :group)
              and (:city is null or h.city = :city)
            order by b.units desc""")
    List<BloodStock> search(String group, String city);
}
