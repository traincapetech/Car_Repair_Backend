package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.*;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.Permission;
import com.carservice.backend.user.enums.RolePermissions;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.enums.UserStatus;
import com.carservice.backend.user.repository.UserRepository;
import com.carservice.backend.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AuditService auditService;

    public AdminUserService(
            UserRepository userRepository,
            WorkshopRepository workshopRepository,
            VehicleRepository vehicleRepository,
            BookingRepository bookingRepository,
            ServiceRequestRepository serviceRequestRepository,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserListResponse> getUsers(
            int page,
            int size,
            String search,
            String role,
            String status,
            String sort,
            String direction
    ) {
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = Math.min(100, Math.max(1, size));

        UserRole roleFilter = null;
        if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) {
            try {
                roleFilter = UserRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid role filter
            }
        }

        UserStatus statusFilter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                statusFilter = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid status filter
            }
        }

        String sortField = "createdAt";
        if (sort != null && !sort.isBlank()) {
            String s = sort.trim();
            if ("name".equalsIgnoreCase(s)) sortField = "name";
            else if ("email".equalsIgnoreCase(s)) sortField = "email";
            else if ("phone".equalsIgnoreCase(s)) sortField = "phone";
            else if ("role".equalsIgnoreCase(s)) sortField = "role";
            else if ("status".equalsIgnoreCase(s)) sortField = "status";
            else if ("updatedAt".equalsIgnoreCase(s)) sortField = "updatedAt";
        }

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(sortDir, sortField));

        String sanitizedSearch = (search != null && !search.trim().isBlank()) ? search.trim() : null;

        Page<User> userPage = userRepository.findUsersWithFilter(roleFilter, statusFilter, null, sanitizedSearch, pageRequest);

        List<AdminUserListResponse> content = userPage.getContent().stream().map(u -> {
            boolean hasWorkshop = (u.getRole() == UserRole.PARTNER || u.getRole() == UserRole.WORKSHOP_OWNER)
                    && workshopRepository.findByUserId(u.getId()).isPresent();

            return new AdminUserListResponse(
                    u.getId(),
                    u.getName(),
                    u.getEmail(),
                    u.getPhone(),
                    u.getRole(),
                    u.getStatus(),
                    u.getIsActive(),
                    hasWorkshop,
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            );
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageRequest, userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminUserSummaryResponse getUserSummary() {
        long total = userRepository.count();
        long active = userRepository.countByIsActiveTrue();
        long suspended = userRepository.countByStatus(UserStatus.SUSPENDED);
        long pending = userRepository.countByStatus(UserStatus.PENDING);
        long inactive = userRepository.countByStatus(UserStatus.INACTIVE);

        long admins = userRepository.countByRoleIn(List.of(
                UserRole.ADMIN,
                UserRole.SUPER_ADMIN,
                UserRole.OPERATIONS_ADMIN,
                UserRole.FINANCE_ADMIN,
                UserRole.SUPPORT_AGENT
        ));

        long partners = userRepository.countByRoleIn(List.of(
                UserRole.PARTNER,
                UserRole.WORKSHOP_OWNER,
                UserRole.WORKSHOP_STAFF
        ));

        long customers = userRepository.countByRole(UserRole.CUSTOMER);

        return new AdminUserSummaryResponse(total, active, suspended, pending, inactive, admins, partners, customers);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = findUserOrThrow(userId);

        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // Computed permissions
        List<String> perms = RolePermissions.getPermissionsForRole(user.getRole()).stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.toList());
        response.setPermissions(perms);

        // Associated workshop profile if partner/owner
        if (user.getRole() == UserRole.PARTNER || user.getRole() == UserRole.WORKSHOP_OWNER) {
            Optional<Workshop> wsOpt = workshopRepository.findByUserId(userId);
            if (wsOpt.isPresent()) {
                Workshop ws = wsOpt.get();
                response.setWorkshop(new AdminUserDetailResponse.WorkshopSummary(
                        ws.getId(),
                        ws.getBusinessName(),
                        ws.getVerificationStatus(),
                        ws.getIsActive(),
                        ws.getCity(),
                        ws.getState(),
                        null,
                        null
                ));
            }
        }

        // Associated customer metrics if customer
        if (user.getRole() == UserRole.CUSTOMER) {
            int vehicles = (int) vehicleRepository.countByUserId(userId);
            int bookings = (int) bookingRepository.countByUserId(userId);
            int requests = (int) serviceRequestRepository.countByUserId(userId);
            response.setCustomer(new AdminUserDetailResponse.CustomerSummary(vehicles, bookings, requests));
        }

        return response;
    }

    @Transactional
    public AdminUserDetailResponse updateUserStatus(Long userId, UpdateUserStatusRequest req, User adminUser) {
        User targetUser = findUserOrThrow(userId);
        UserStatus currentStatus = targetUser.getStatus();
        UserStatus targetStatus = req.getStatus();

        if (currentStatus == targetStatus) {
            throw new IllegalArgumentException("User is already in status " + currentStatus);
        }

        // Privileged Safeguard 1: Normal ADMIN cannot modify a SUPER_ADMIN account
        if (targetUser.getRole() == UserRole.SUPER_ADMIN && (adminUser == null || adminUser.getRole() != UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Only a SUPER_ADMIN can modify a SUPER_ADMIN account");
        }

        // Privileged Safeguard 2: Admin cannot deactivate or suspend their own active session
        if (adminUser != null && targetUser.getId().equals(adminUser.getId()) && targetStatus != UserStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deactivate or suspend your own active administrator account");
        }

        // Privileged Safeguard 3: Cannot deactivate or suspend the last active SUPER_ADMIN
        if (targetUser.getRole() == UserRole.SUPER_ADMIN && targetStatus != UserStatus.ACTIVE) {
            long activeSuperAdmins = userRepository.countByRoleAndIsActiveTrue(UserRole.SUPER_ADMIN);
            if (activeSuperAdmins <= 1) {
                throw new IllegalStateException("Cannot deactivate or suspend the platform's last active SUPER_ADMIN");
            }
        }

        boolean wasActive = Boolean.TRUE.equals(targetUser.getIsActive());
        targetUser.setStatus(targetStatus);
        userRepository.save(targetUser);

        String desc = String.format("User [%s] (ID: %d, Role: %s) status changed from %s to %s. Reason: %s",
                targetUser.getEmail(), targetUser.getId(), targetUser.getRole(), currentStatus, targetStatus, req.getReason());

        auditService.record(
                adminUser,
                "USER_STATUS_CHANGED",
                "USER",
                String.valueOf(userId),
                desc,
                "SUCCESS",
                Map.of("id", userId, "status", currentStatus.name(), "isActive", wasActive),
                Map.of("id", userId, "status", targetStatus.name(), "isActive", targetUser.getIsActive()),
                Map.of("reason", req.getReason(), "userEmail", targetUser.getEmail(), "role", targetUser.getRole().name())
        );

        log.info("AUDIT: Admin [{}] changed user [{}] status from [{}] to [{}] (isActive={}) with reason: {}",
                adminUser != null ? adminUser.getEmail() : "SYSTEM",
                targetUser.getEmail(),
                currentStatus,
                targetStatus,
                targetUser.getIsActive(),
                req.getReason());

        return getUserDetail(userId);
    }

    @Transactional
    public AdminUserDetailResponse updateUserRole(Long userId, UpdateUserRoleRequest req, User adminUser) {
        // Privileged Safeguard: Only SUPER_ADMIN can assign or modify user roles
        if (adminUser == null || adminUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Only a SUPER_ADMIN can assign or modify user roles");
        }

        User targetUser = findUserOrThrow(userId);
        UserRole oldRole = targetUser.getRole();
        UserRole newRole = req.getRole();

        if (oldRole == newRole) {
            throw new IllegalArgumentException("User already has role " + newRole);
        }

        // Privileged Safeguard: Cannot revoke own SUPER_ADMIN role if currently logged in
        if (targetUser.getId().equals(adminUser.getId()) && newRole != UserRole.SUPER_ADMIN) {
            throw new IllegalStateException("Cannot revoke your own SUPER_ADMIN role");
        }

        // Privileged Safeguard: Cannot downgrade the platform's last active SUPER_ADMIN
        if (oldRole == UserRole.SUPER_ADMIN && newRole != UserRole.SUPER_ADMIN) {
            long activeSuperAdmins = userRepository.countByRoleAndIsActiveTrue(UserRole.SUPER_ADMIN);
            if (activeSuperAdmins <= 1) {
                throw new IllegalStateException("Cannot downgrade the platform's last active SUPER_ADMIN account");
            }
        }

        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        String desc = String.format("User [%s] (ID: %d) role changed from %s to %s. Reason: %s",
                targetUser.getEmail(), targetUser.getId(), oldRole, newRole, req.getReason());

        auditService.record(
                adminUser,
                "USER_ROLE_CHANGED",
                "USER",
                String.valueOf(userId),
                desc,
                "SUCCESS",
                Map.of("id", userId, "role", oldRole.name()),
                Map.of("id", userId, "role", newRole.name()),
                Map.of("reason", req.getReason(), "userEmail", targetUser.getEmail())
        );

        log.info("AUDIT: Super Admin [{}] changed user [{}] role from [{}] to [{}] with reason: {}",
                adminUser.getEmail(),
                targetUser.getEmail(),
                oldRole,
                newRole,
                req.getReason());

        return getUserDetail(userId);
    }

    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getRoles() {
        List<AdminRoleResponse> roles = new ArrayList<>();

        for (UserRole role : UserRole.values()) {
            long userCount = userRepository.countByRole(role);
            boolean isPrivileged = (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN || role == UserRole.OPERATIONS_ADMIN || role == UserRole.FINANCE_ADMIN);

            List<AdminRoleResponse.PermissionDto> permissionDtos = RolePermissions.getPermissionsForRole(role).stream()
                    .sorted(Comparator.comparing(Permission::getModule).thenComparing(Permission::name))
                    .map(p -> new AdminRoleResponse.PermissionDto(p.name(), p.getModule(), p.getDescription()))
                    .collect(Collectors.toList());

            roles.add(new AdminRoleResponse(
                    role.name(),
                    formatRoleName(role),
                    getRoleDescription(role),
                    isPrivileged,
                    userCount,
                    permissionDtos
            ));
        }

        return roles;
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private String formatRoleName(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super Administrator";
            case ADMIN -> "Administrator";
            case OPERATIONS_ADMIN -> "Operations Administrator";
            case FINANCE_ADMIN -> "Finance Administrator";
            case SUPPORT_AGENT -> "Support Agent";
            case WORKSHOP_OWNER -> "Workshop Owner";
            case PARTNER -> "Workshop Partner";
            case WORKSHOP_STAFF -> "Workshop Staff";
            case CUSTOMER -> "Customer";
        };
    }

    private String getRoleDescription(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> "Full access across all platform modules, system configurations, and administrator role assignments.";
            case ADMIN -> "Operational management across users, customers, workshops, service catalogs, and platform auditing.";
            case OPERATIONS_ADMIN -> "Fulfillment and dispatch oversight, managing matching rules, workshops, and customer bookings.";
            case FINANCE_ADMIN -> "Financial visibility and controls for workshop wallets, fee transactions, refunds, and payment audits.";
            case SUPPORT_AGENT -> "Customer service and support representative managing customer profiles and booking inquiries.";
            case WORKSHOP_OWNER -> "Independent garage or service centre owner managing facility bookings, leads, and earnings.";
            case PARTNER -> "Automotive service partner facility profile and lead management.";
            case WORKSHOP_STAFF -> "Service centre technician or representative assigned to fulfillment tasks.";
            case CUSTOMER -> "Vehicle owner booking vehicle maintenance, repairs, and doorstep services.";
        };
    }
}
