package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.exception.ResourceNotFoundException;
import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.dto.PartnerWorkshopProfileResponse;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partner/workshop")
public class PartnerProfileController {

    private final WorkshopRepository workshopRepository;

    public PartnerProfileController(WorkshopRepository workshopRepository) {
        this.workshopRepository = workshopRepository;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PARTNER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PartnerWorkshopProfileResponse>> getProfile(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No workshop partner account found for user: " + currentUser.getEmail()));

        PartnerWorkshopProfileResponse response = new PartnerWorkshopProfileResponse();
        response.setId(workshop.getId());
        response.setUserId(workshop.getUser() != null ? workshop.getUser().getId() : null);
        response.setBusinessName(workshop.getBusinessName());
        response.setOwnerName(workshop.getOwnerName());
        response.setEmail(workshop.getEmail());
        response.setPhone(workshop.getPhone());
        response.setAddress(workshop.getAddress());
        response.setCity(workshop.getCity());
        response.setState(workshop.getState());
        response.setPincode(workshop.getPincode());
        response.setLatitude(workshop.getLatitude());
        response.setLongitude(workshop.getLongitude());
        response.setOpeningTime(workshop.getOpeningTime());
        response.setClosingTime(workshop.getClosingTime());
        response.setWorkingDays(workshop.getWorkingDays());
        response.setIsActive(workshop.getIsActive());
        response.setVerificationStatus(workshop.getVerificationStatus());
        response.setRejectionReason(workshop.getStatusReason());
        response.setApprovedAt(workshop.getApprovedAt());
        response.setApprovedBy(workshop.getApprovedBy() != null ? String.valueOf(workshop.getApprovedBy()) : null);
        response.setCreatedAt(workshop.getCreatedAt());

        return ResponseEntity.ok(ApiResponse.success("Partner workshop profile retrieved successfully", response));
    }
}
