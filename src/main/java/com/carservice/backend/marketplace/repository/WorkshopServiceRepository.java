package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkshopServiceRepository extends JpaRepository<WorkshopService, Long> {

    List<WorkshopService> findByWorkshopIdAndIsActiveTrue(Long workshopId);

    List<WorkshopService> findByWorkshopId(Long workshopId);

    boolean existsByWorkshopIdAndServiceCatalogIdAndIsActiveTrue(Long workshopId, Long serviceCatalogId);

    @Query("SELECT COUNT(ws) FROM WorkshopService ws WHERE ws.workshop.id = :workshopId AND ws.isActive = true AND ws.serviceCatalog.id IN :serviceIds")
    long countActiveServicesByWorkshopAndServiceIds(
            @Param("workshopId") Long workshopId,
            @Param("serviceIds") Collection<Long> serviceIds
    );

    @Query("SELECT ws.workshop.id, COUNT(ws) FROM WorkshopService ws WHERE ws.workshop.id IN :workshopIds AND ws.isActive = true GROUP BY ws.workshop.id")
    List<Object[]> countActiveServicesByWorkshopIds(@Param("workshopIds") Collection<Long> workshopIds);

    @Query("SELECT ws FROM WorkshopService ws JOIN FETCH ws.serviceCatalog sc WHERE ws.workshop.id = :workshopId ORDER BY sc.category ASC, sc.name ASC")
    List<WorkshopService> findByWorkshopIdWithCatalog(@Param("workshopId") Long workshopId);

    boolean existsByServiceCatalogId(Long serviceCatalogId);
}
