package com.carservice.backend.servicecatalog.repository;

import com.carservice.backend.servicecatalog.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {

    List<ServiceCatalog> findAllByOrderByNameAsc();

    List<ServiceCatalog> findAllByIsActiveTrueOrderByNameAsc();

    Optional<ServiceCatalog> findByIdAndIsActiveTrue(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
