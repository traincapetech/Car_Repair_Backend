package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import com.carservice.backend.vehicle.entity.Vehicle;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminCustomerService {

    private static final Logger log = LoggerFactory.getLogger(AdminCustomerService.class);

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public AdminCustomerService(
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            BookingRepository bookingRepository,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminCustomerListResponse> getCustomers(
            int page,
            int size,
            String search,
            String status,
            String sort,
            String direction
    ) {
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Boolean activeFilter = null;
        if (status != null && !status.isBlank()) {
            String trimmed = status.trim().toUpperCase();
            if ("ACTIVE".equals(trimmed) || "TRUE".equals(trimmed)) {
                activeFilter = Boolean.TRUE;
            } else if ("INACTIVE".equals(trimmed) || "FALSE".equals(trimmed)) {
                activeFilter = Boolean.FALSE;
            }
        }

        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;

        String sortProp = "createdAt";
        if (sort != null) {
            String s = sort.trim();
            if (s.equalsIgnoreCase("name") || s.equalsIgnoreCase("email") || s.equalsIgnoreCase("updatedAt") || s.equalsIgnoreCase("createdAt")) {
                sortProp = s;
            }
        }

        Sort.Direction sortDirection = (direction != null && direction.trim().equalsIgnoreCase("ASC"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDirection, sortProp));
        Page<User> userPage = userRepository.findCustomersWithFilter(activeFilter, searchFilter, pageable);

        if (userPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, userPage.getTotalElements());
        }

        List<Long> customerIds = userPage.getContent().stream().map(User::getId).toList();

        // Batch count queries - 0 N+1 overhead
        Map<Long, Long> vehicleCounts = toCountMap(vehicleRepository.countVehiclesByUserIds(customerIds));
        Map<Long, Long> bookingCounts = toCountMap(bookingRepository.countBookingsByUserIds(customerIds));
        Map<Long, Long> srCounts = toCountMap(serviceRequestRepository.countServiceRequestsByUserIds(customerIds));

        List<AdminCustomerListResponse> responseList = userPage.getContent().stream().map(u -> new AdminCustomerListResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                u.getIsActive(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                vehicleCounts.getOrDefault(u.getId(), 0L),
                bookingCounts.getOrDefault(u.getId(), 0L),
                srCounts.getOrDefault(u.getId(), 0L)
        )).toList();

        return new PageImpl<>(responseList, pageable, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminCustomerDetailResponse getCustomerDetail(Long customerId) {
        User customer = findCustomerOrThrow(customerId);

        AdminCustomerDetailResponse response = new AdminCustomerDetailResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setRole(customer.getRole());
        response.setIsActive(customer.getIsActive());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());

        // Registered vehicles
        List<Vehicle> vehicles = vehicleRepository.findAllByUserIdOrderByCreatedAtDesc(customerId);
        response.setTotalVehicles(vehicles.size());
        response.setVehicles(vehicles.stream().map(this::mapToVehicleResponse).toList());

        // Booking statistics by status
        List<Object[]> bookingStatusCounts = bookingRepository.countBookingsByStatusForUser(customerId);
        long totalBookings = 0;
        for (Object[] row : bookingStatusCounts) {
            BookingStatus bStatus = (BookingStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalBookings += count;
            if (bStatus == BookingStatus.PENDING) response.setPendingBookings(count);
            else if (bStatus == BookingStatus.CONFIRMED) response.setConfirmedBookings(count);
            else if (bStatus == BookingStatus.IN_PROGRESS) response.setInProgressBookings(count);
            else if (bStatus == BookingStatus.COMPLETED) response.setCompletedBookings(count);
            else if (bStatus == BookingStatus.CANCELLED) response.setCancelledBookings(count);
        }
        response.setTotalBookings(totalBookings);

        // Service request statistics by status
        List<Object[]> srStatusCounts = serviceRequestRepository.countServiceRequestsByStatusForUser(customerId);
        long totalSr = 0;
        for (Object[] row : srStatusCounts) {
            ServiceRequestStatus srStatus = (ServiceRequestStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalSr += count;
            if (srStatus == ServiceRequestStatus.SUBMITTED) response.setSubmittedRequests(count);
            else if (srStatus == ServiceRequestStatus.MATCHED || srStatus == ServiceRequestStatus.ACCEPTED) response.setMatchedRequests(count);
            else if (srStatus == ServiceRequestStatus.IN_PROGRESS) response.setInProgressRequests(count);
            else if (srStatus == ServiceRequestStatus.COMPLETED) response.setCompletedRequests(count);
            else if (srStatus == ServiceRequestStatus.CANCELLED) response.setCancelledRequests(count);
        }
        response.setTotalServiceRequests(totalSr);

        return response;
    }

    @Transactional(readOnly = true)
    public List<AdminCustomerVehicleResponse> getCustomerVehicles(Long customerId) {
        findCustomerOrThrow(customerId);
        return vehicleRepository.findAllByUserIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToVehicleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminCustomerBookingResponse> getCustomerBookings(
            Long customerId,
            int page,
            int size,
            BookingStatus status,
            String sort,
            String direction
    ) {
        findCustomerOrThrow(customerId);

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        String sortProp = "createdAt";
        if (sort != null && (sort.equalsIgnoreCase("bookingDate") || sort.equalsIgnoreCase("createdAt"))) {
            sortProp = sort;
        }

        Sort.Direction sortDirection = (direction != null && direction.trim().equalsIgnoreCase("ASC"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDirection, sortProp));
        Page<Booking> bookingPage = bookingRepository.findByUserIdAndOptionalStatus(customerId, status, pageable);

        List<AdminCustomerBookingResponse> dtoList = bookingPage.getContent().stream().map(this::mapToBookingResponse).toList();
        return new PageImpl<>(dtoList, pageable, bookingPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AdminCustomerServiceRequestResponse> getCustomerServiceRequests(
            Long customerId,
            int page,
            int size,
            ServiceRequestStatus status,
            String direction
    ) {
        findCustomerOrThrow(customerId);

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        Sort.Direction sortDirection = (direction != null && direction.trim().equalsIgnoreCase("ASC"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDirection, "createdAt"));
        Page<ServiceRequest> srPage = serviceRequestRepository.findByUserIdAndOptionalStatus(customerId, status, pageable);

        List<AdminCustomerServiceRequestResponse> dtoList = srPage.getContent().stream().map(this::mapToServiceRequestResponse).toList();
        return new PageImpl<>(dtoList, pageable, srPage.getTotalElements());
    }

    @Transactional
    public AdminCustomerDetailResponse updateCustomerStatus(
            Long customerId,
            UpdateCustomerStatusRequest request,
            User adminUser
    ) {
        // Self-protection check
        if (adminUser != null && adminUser.getId() != null && adminUser.getId().equals(customerId)) {
            throw new IllegalArgumentException("Administrators cannot deactivate their own account through customer management");
        }

        // Role protection & existence check
        User customer = findCustomerOrThrow(customerId);

        boolean currentStatus = Boolean.TRUE.equals(customer.getIsActive());
        boolean targetStatus = Boolean.TRUE.equals(request.getIsActive());

        if (currentStatus == targetStatus) {
            String stateDesc = currentStatus ? "active" : "inactive";
            throw new IllegalArgumentException("Customer account is already " + stateDesc);
        }

        customer.setIsActive(targetStatus);
        userRepository.save(customer);

        log.info("AUDIT: Admin [id={}, email={}] changed customer [id={}, email={}] account status from [{}] to [{}]",
                adminUser != null ? adminUser.getId() : "SYSTEM",
                adminUser != null ? adminUser.getEmail() : "SYSTEM",
                customer.getId(),
                customer.getEmail(),
                currentStatus ? "ACTIVE" : "INACTIVE",
                targetStatus ? "ACTIVE" : "INACTIVE"
        );

        return getCustomerDetail(customerId);
    }

    private User findCustomerOrThrow(Long customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (user.getRole() != UserRole.CUSTOMER) {
            // Do not leak existence of admin/partner accounts via customer endpoints
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return user;
    }

    private AdminCustomerVehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        if (vehicle == null) return null;
        return new AdminCustomerVehicleResponse(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getRegistrationNumber(),
                vehicle.getFuelType(),
                vehicle.getTransmission(),
                vehicle.getCreatedAt()
        );
    }

    private AdminCustomerBookingResponse mapToBookingResponse(Booking b) {
        AdminCustomerBookingResponse dto = new AdminCustomerBookingResponse();
        dto.setId(b.getId());
        dto.setBookingReference(b.getBookingReference());
        dto.setVehicle(mapToVehicleResponse(b.getVehicle()));
        
        String serviceName = b.getServiceNameSnapshot();
        if (serviceName == null && b.getService() != null) {
            serviceName = b.getService().getName();
        }
        dto.setServiceName(serviceName);
        dto.setServicePriceSnapshot(b.getServicePriceSnapshot());
        dto.setEstimatedPrice(b.getEstimatedPrice());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setBookingDate(b.getBookingDate());
        dto.setBookingTime(b.getBookingTime());
        dto.setTimeSlot(b.getTimeSlot());
        dto.setStatus(b.getStatus());
        dto.setCustomerNotes(b.getCustomerNotes());
        dto.setCity(b.getCity());
        dto.setAddress(b.getAddress());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setCancelledAt(b.getCancelledAt());
        return dto;
    }

    private AdminCustomerServiceRequestResponse mapToServiceRequestResponse(ServiceRequest sr) {
        AdminCustomerServiceRequestResponse dto = new AdminCustomerServiceRequestResponse();
        dto.setId(sr.getId());
        dto.setRequestReference(sr.getRequestReference());
        dto.setVehicle(mapToVehicleResponse(sr.getVehicle()));
        dto.setCity(sr.getCity());
        dto.setAddress(sr.getAddress());
        dto.setPincode(sr.getPincode());
        dto.setPreferredDate(sr.getPreferredDate());
        dto.setPreferredTimeSlot(sr.getPreferredTimeSlot());
        dto.setStatus(sr.getStatus());
        dto.setTotalAmount(sr.getTotalAmount());
        dto.setBookingReference(sr.getBookingReference());
        if (sr.getAssignedWorkshop() != null) {
            dto.setAssignedWorkshopId(sr.getAssignedWorkshop().getId());
            dto.setAssignedWorkshopName(sr.getAssignedWorkshop().getBusinessName());
        }
        if (sr.getItems() != null && !sr.getItems().isEmpty()) {
            String services = sr.getItems().stream()
                    .map(item -> item.getServiceNameSnapshot())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            dto.setRequestedServices(services);
        }
        dto.setCreatedAt(sr.getCreatedAt());
        dto.setUpdatedAt(sr.getUpdatedAt());
        return dto;
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        return map;
    }
}
