package com.mediconnect.lab;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    @Query("""
            select t from LabTest t
            where (:includeInactive = true or t.active = true)
              and (:category is null or t.category = :category)
              and (:q is null or lower(t.name) like :q or lower(t.description) like :q)
            order by t.category, t.price""")
    List<LabTest> search(String category, String q, boolean includeInactive);

    @Query("select distinct t.category from LabTest t where t.active = true order by t.category")
    List<String> findCategories();

    boolean existsByCodeIgnoreCase(String code);
}
