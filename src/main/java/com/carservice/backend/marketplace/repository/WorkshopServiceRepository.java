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
}
