package com.mediconnect.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrganAvailabilityRepository extends JpaRepository<OrganAvailability, Long> {

    @Query("""
            select o from OrganAvailability o join fetch o.hospital h
            where (:organ is null or o.organ = :organ)
              and (:city is null or h.city = :city)
            order by o.available desc""")
    List<OrganAvailability> search(String organ, String city);

    @Query("select distinct o.organ from OrganAvailability o order by o.organ")
    List<String> findOrgans();
}
