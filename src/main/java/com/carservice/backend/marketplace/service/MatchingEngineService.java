package com.carservice.backend.marketplace.service;

import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.ServiceRequestItem;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.marketplace.repository.WorkshopServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingEngineService {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_SERVICE_RADIUS_KM = 25.0;

    private final WorkshopRepository workshopRepository;
    private final WorkshopServiceRepository workshopServiceRepository;

    public MatchingEngineService(
            WorkshopRepository workshopRepository,
            WorkshopServiceRepository workshopServiceRepository
    ) {
        this.workshopRepository = workshopRepository;
        this.workshopServiceRepository = workshopServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<Workshop> findEligibleWorkshops(ServiceRequest serviceRequest, Collection<Long> excludedWorkshopIds) {
        if (serviceRequest == null) {
            return Collections.emptyList();
        }

        Set<Long> excluded = excludedWorkshopIds != null ? new HashSet<>(excludedWorkshopIds) : Collections.emptySet();

        // 1. Get all active and verified workshops
        List<Workshop> activeWorkshops = workshopRepository.findByIsActiveTrueAndVerificationStatus(WorkshopVerificationStatus.VERIFIED);
        if (activeWorkshops.isEmpty()) {
            log.info("No active verified workshops found in system for request {}", serviceRequest.getRequestReference());
            return Collections.emptyList();
        }

        // 2. Collect requested service IDs
        Set<Long> requiredServiceIds = serviceRequest.getItems().stream()
                .map(ServiceRequestItem::getServiceCatalog)
                .filter(Objects::nonNull)
                .map(s -> s.getId())
                .collect(Collectors.toSet());

        log.debug("Matching request {} requiring {} services: {}",
                serviceRequest.getRequestReference(), requiredServiceIds.size(), requiredServiceIds);

        List<Workshop> matchedWorkshops = new ArrayList<>();

        for (Workshop workshop : activeWorkshops) {
            // Check exclusion
            if (excluded.contains(workshop.getId())) {
                log.debug("Workshop {} is excluded from matching for request {}", workshop.getId(), serviceRequest.getRequestReference());
                continue;
            }

            // 3. 100% Service Coverage Check
            if (!requiredServiceIds.isEmpty()) {
                long coveredCount = workshopServiceRepository.countActiveServicesByWorkshopAndServiceIds(
                        workshop.getId(),
                        requiredServiceIds
                );
                if (coveredCount < requiredServiceIds.size()) {
                    log.debug("Workshop {} offers only {}/{} requested services - skipping",
                            workshop.getId(), coveredCount, requiredServiceIds.size());
                    continue;
                }
            }

            // 4. Location Match Check
            boolean locationMatches = evaluateLocationMatch(serviceRequest, workshop);
            if (locationMatches) {
                matchedWorkshops.add(workshop);
            }
        }

        log.info("Matching engine found {} eligible workshops for request {}",
                matchedWorkshops.size(), serviceRequest.getRequestReference());

        return matchedWorkshops;
    }

    public boolean evaluateLocationMatch(ServiceRequest serviceRequest, Workshop workshop) {
        BigDecimal reqLat = serviceRequest.getLatitude();
        BigDecimal reqLng = serviceRequest.getLongitude();
        BigDecimal wLat = workshop.getLatitude();
        BigDecimal wLng = workshop.getLongitude();

        // If coordinates are available on both sides, compute Haversine distance
        if (reqLat != null && reqLng != null && wLat != null && wLng != null) {
            double distanceKm = calculateHaversineDistance(
                    reqLat.doubleValue(), reqLng.doubleValue(),
                    wLat.doubleValue(), wLng.doubleValue()
            );

            double allowedRadiusKm = workshop.getServiceRadiusKm() != null
                    ? workshop.getServiceRadiusKm().doubleValue()
                    : DEFAULT_SERVICE_RADIUS_KM;

            boolean inRange = distanceKm <= allowedRadiusKm;
            log.debug("Workshop {} distance: {} km, allowed radius: {} km -> inRange: {}",
                    workshop.getBusinessName(), String.format("%.2f", distanceKm), allowedRadiusKm, inRange);

            if (inRange) {
                return true;
            }
            // If coordinate check explicitly fails distance test, do NOT fallback to entire city
            return false;
        }

        // Fallback: Case-insensitive exact city match
        if (serviceRequest.getCity() != null && workshop.getCity() != null) {
            boolean cityMatch = serviceRequest.getCity().trim().equalsIgnoreCase(workshop.getCity().trim());
            log.debug("Workshop {} city match: request='{}', workshop='{}' -> {}",
                    workshop.getBusinessName(), serviceRequest.getCity(), workshop.getCity(), cityMatch);
            return cityMatch;
        }

        return false;
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
