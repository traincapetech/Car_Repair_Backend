package com.carservice.backend.marketplace.repository;

import com.carservice.backend.marketplace.entity.WorkshopJob;
import com.carservice.backend.marketplace.enums.WorkshopJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopJobRepository extends JpaRepository<WorkshopJob, Long> {

    Optional<WorkshopJob> findByJobReference(String jobReference);

    Optional<WorkshopJob> findByServiceRequestId(Long serviceRequestId);

    Optional<WorkshopJob> findByOpportunityId(Long opportunityId);

    List<WorkshopJob> findByWorkshopIdOrderByCreatedAtDesc(Long workshopId);

    List<WorkshopJob> findByWorkshopIdAndStatusOrderByCreatedAtDesc(Long workshopId, WorkshopJobStatus status);

    @Query("SELECT j FROM WorkshopJob j WHERE j.workshop.user.id = :userId ORDER BY j.createdAt DESC")
    List<WorkshopJob> findByWorkshopUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT j FROM WorkshopJob j WHERE j.serviceRequest.user.id = :userId ORDER BY j.createdAt DESC")
    List<WorkshopJob> findByCustomerUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT j FROM WorkshopJob j WHERE j.serviceRequest.user.id = :userId AND j.status NOT IN ('COMPLETED', 'CANCELLED', 'TRANSFERRED') ORDER BY j.createdAt DESC")
    List<WorkshopJob> findActiveJobsByCustomerUserId(@Param("userId") Long userId);

    long countByWorkshopIdAndStatus(Long workshopId, WorkshopJobStatus status);

    long countByWorkshopId(Long workshopId);

    @Query("SELECT j.workshop.id, COUNT(j) FROM WorkshopJob j WHERE j.workshop.id IN :workshopIds GROUP BY j.workshop.id")
    List<Object[]> countJobsByWorkshopIds(@Param("workshopIds") java.util.Collection<Long> workshopIds);

    @Query("SELECT j.workshop.id, COUNT(j) FROM WorkshopJob j WHERE j.workshop.id IN :workshopIds AND j.status = com.carservice.backend.marketplace.enums.WorkshopJobStatus.COMPLETED GROUP BY j.workshop.id")
    List<Object[]> countCompletedJobsByWorkshopIds(@Param("workshopIds") java.util.Collection<Long> workshopIds);

    @Query(value = """
        SELECT j FROM WorkshopJob j
        JOIN FETCH j.serviceRequest sr
        LEFT JOIN FETCH sr.vehicle v
        WHERE j.workshop.id = :workshopId
          AND (:status IS NULL OR j.status = :status)
        ORDER BY j.createdAt DESC
    """,
    countQuery = """
        SELECT COUNT(j) FROM WorkshopJob j
        WHERE j.workshop.id = :workshopId
          AND (:status IS NULL OR j.status = :status)
    """)
    org.springframework.data.domain.Page<WorkshopJob> findByWorkshopIdAndOptionalStatus(
            @Param("workshopId") Long workshopId,
            @Param("status") WorkshopJobStatus status,
            org.springframework.data.domain.Pageable pageable
    );
}
