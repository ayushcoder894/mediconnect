package com.mediconnect.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findAllByOrderByCityAscNameAsc();

    @Query("select distinct h.city from Hospital h order by h.city")
    List<String> findCities();
}
