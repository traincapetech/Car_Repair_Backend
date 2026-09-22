package com.carservice.backend.admin.service;

import com.carservice.backend.admin.dto.reports.*;
import com.carservice.backend.booking.entity.Booking;
import com.carservice.backend.booking.enums.BookingStatus;
import com.carservice.backend.booking.repository.BookingRepository;
import com.carservice.backend.booking.repository.BookingServiceRepository;
import com.carservice.backend.marketplace.entity.ServiceRequest;
import com.carservice.backend.marketplace.entity.Workshop;
import com.carservice.backend.marketplace.enums.ServiceRequestStatus;
import com.carservice.backend.marketplace.enums.WorkshopVerificationStatus;
import com.carservice.backend.marketplace.repository.ServiceRequestItemRepository;
import com.carservice.backend.marketplace.repository.ServiceRequestRepository;
import com.carservice.backend.marketplace.repository.WorkshopRepository;
import com.carservice.backend.servicecatalog.repository.ServiceCatalogRepository;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminReportsService {

    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final BookingRepository bookingRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceRequestItemRepository serviceRequestItemRepository;
    private final BookingServiceRepository bookingServiceRepository;

    public AdminReportsService(
            UserRepository userRepository,
            WorkshopRepository workshopRepository,
            ServiceRequestRepository serviceRequestRepository,
            BookingRepository bookingRepository,
            ServiceCatalogRepository serviceCatalogRepository,
            ServiceRequestItemRepository serviceRequestItemRepository,
            BookingServiceRepository bookingServiceRepository
    ) {
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.bookingRepository = bookingRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.serviceRequestItemRepository = serviceRequestItemRepository;
        this.bookingServiceRepository = bookingServiceRepository;
    }

    public AdminPlatformOverviewReport getPlatformOverview(LocalDateTime from, LocalDateTime to) {
        AdminPlatformOverviewReport report = new AdminPlatformOverviewReport();

        // Lifetime counters
        report.setTotalCustomers(userRepository.countByRole(UserRole.CUSTOMER));
        report.setTotalWorkshops(workshopRepository.count());
        report.setVerifiedWorkshops(workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.VERIFIED));
        report.setPendingWorkshopApprovals(workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.PENDING));
        report.setActiveServiceCatalogItems(serviceCatalogRepository.countByIsActiveTrue());
        report.setTotalServiceRequests(serviceRequestRepository.count());
        report.setTotalBookings(bookingRepository.count());
        report.setCompletedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED));
        report.setCancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED));

        // Period-specific counters
        if (from != null || to != null) {
            report.setPeriodCustomers(userRepository.countCustomersByDateRange(from, to));
            report.setPeriodWorkshops(workshopRepository.countWorkshopsByDateRange(from, to));
            report.setPeriodServiceRequests(serviceRequestRepository.countServiceRequestsFiltered(from, to, null, null));
            report.setPeriodBookings(bookingRepository.countBookingsFiltered(from, to, null, null));
            report.setPeriodCompletedBookings(bookingRepository.countBookingsFiltered(from, to, BookingStatus.COMPLETED, null));
            report.setPeriodCancelledBookings(bookingRepository.countBookingsFiltered(from, to, BookingStatus.CANCELLED, null));
        } else {
            report.setPeriodCustomers(report.getTotalCustomers());
            report.setPeriodWorkshops(report.getTotalWorkshops());
            report.setPeriodServiceRequests(report.getTotalServiceRequests());
            report.setPeriodBookings(report.getTotalBookings());
            report.setPeriodCompletedBookings(report.getCompletedBookings());
            report.setPeriodCancelledBookings(report.getCancelledBookings());
        }

        return report;
    }

    public AdminCustomerReport getCustomerReport(LocalDateTime from, LocalDateTime to) {
        AdminCustomerReport report = new AdminCustomerReport();

        long total = userRepository.countByRole(UserRole.CUSTOMER);
        long active = userRepository.countByRoleAndIsActiveTrue(UserRole.CUSTOMER);
        long inactive = userRepository.countCustomersByActiveAndDateRange(false, null, null);
        long newCustomers = userRepository.countCustomersByDateRange(from, to);

        report.setTotalCustomers(total);
        report.setActiveCustomers(active);
        report.setInactiveSuspendedCustomers(inactive);
        report.setNewCustomers(newCustomers);

        if (total > 0) {
            double rate = (newCustomers * 100.0) / total;
            report.setGrowthRatePercentage(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue());
        } else {
            report.setGrowthRatePercentage(0.0);
        }

        List<Object[]> rows = userRepository.countCustomerRegistrationsByDayNative(from, to);
        List<AdminReportTimelinePoint> points = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String dateStr = String.valueOf(row[0]);
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                points.add(new AdminReportTimelinePoint(dateStr, count));
            }
        }
        report.setTimeline(points);

        return report;
    }

    public AdminWorkshopReport getWorkshopReport(LocalDateTime from, LocalDateTime to) {
        AdminWorkshopReport report = new AdminWorkshopReport();

        report.setTotalWorkshops(workshopRepository.count());
        report.setPendingApproval(workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.PENDING));
        report.setVerifiedWorkshops(workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.VERIFIED));
        report.setRejectedWorkshops(workshopRepository.countByVerificationStatus(WorkshopVerificationStatus.REJECTED));
        report.setActiveWorkshops(workshopRepository.countByIsActiveTrue());
        report.setInactiveWorkshops(workshopRepository.countWorkshopsByActiveAndDateRange(false, null, null));

        boolean hasCoords = workshopRepository.countWorkshopsWithCoordinates() > 0;
        report.setHasGeoCoordinates(hasCoords);

        List<Object[]> rows = workshopRepository.countWorkshopRegistrationsByDayNative(from, to);
        List<AdminReportTimelinePoint> points = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String dateStr = String.valueOf(row[0]);
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                points.add(new AdminReportTimelinePoint(dateStr, count));
            }
        }
        report.setTimeline(points);

        List<Object[]> cityRows = workshopRepository.countWorkshopsByCityAndState();
        List<AdminWorkshopReport.CityDistributionItem> cityItems = new ArrayList<>();
        if (cityRows != null) {
            for (Object[] row : cityRows) {
                String city = row[0] != null ? String.valueOf(row[0]) : "Unknown";
                String state = row[1] != null ? String.valueOf(row[1]) : "";
                long count = row[2] instanceof Number ? ((Number) row[2]).longValue() : 0L;
                cityItems.add(new AdminWorkshopReport.CityDistributionItem(city, state, count));
            }
        }
        report.setCityDistribution(cityItems);

        return report;
    }

    public AdminServiceRequestReport getServiceRequestReport(LocalDateTime from, LocalDateTime to, Long workshopId) {
        AdminServiceRequestReport report = new AdminServiceRequestReport();

        long total = serviceRequestRepository.countServiceRequestsFiltered(from, to, null, workshopId);
        report.setTotalRequests(total);

        List<Object[]> statusRows = serviceRequestRepository.countServiceRequestsGroupedByStatus(from, to, workshopId);
        Map<String, Long> countMap = new HashMap<>();
        if (statusRows != null) {
            for (Object[] row : statusRows) {
                if (row[0] != null) {
                    String statusName = row[0].toString();
                    long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                    countMap.put(statusName, count);
                }
            }
        }

        report.setSubmitted(countMap.getOrDefault("SUBMITTED", 0L));
        report.setMatched(countMap.getOrDefault("MATCHED", 0L));
        report.setAccepted(countMap.getOrDefault("ACCEPTED", 0L));
        report.setInProgress(countMap.getOrDefault("IN_PROGRESS", 0L));
        report.setCompleted(countMap.getOrDefault("COMPLETED", 0L));
        report.setCancelled(countMap.getOrDefault("CANCELLED", 0L));
        report.setReMatching(countMap.getOrDefault("RE_MATCHING", 0L));

        Map<String, Double> distribution = new LinkedHashMap<>();
        if (total > 0) {
            for (ServiceRequestStatus s : ServiceRequestStatus.values()) {
                long c = countMap.getOrDefault(s.name(), 0L);
                double pct = (c * 100.0) / total;
                distribution.put(s.name(), BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
            }
        }
        report.setStatusDistribution(distribution);

        List<Object[]> rows = serviceRequestRepository.countServiceRequestsByDayNative(from, to, workshopId);
        List<AdminReportTimelinePoint> points = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String dateStr = String.valueOf(row[0]);
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                points.add(new AdminReportTimelinePoint(dateStr, count));
            }
        }
        report.setTimeline(points);

        return report;
    }

    public AdminBookingReport getBookingReport(LocalDateTime from, LocalDateTime to, Long workshopId) {
        AdminBookingReport report = new AdminBookingReport();

        long total = bookingRepository.countBookingsFiltered(from, to, null, workshopId);
        report.setTotalBookings(total);

        List<Object[]> statusRows = bookingRepository.countBookingsGroupedByStatus(from, to, workshopId);
        Map<String, Long> countMap = new HashMap<>();
        if (statusRows != null) {
            for (Object[] row : statusRows) {
                if (row[0] != null) {
                    String statusName = row[0].toString();
                    long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                    countMap.put(statusName, count);
                }
            }
        }

        report.setPending(countMap.getOrDefault("PENDING", 0L));
        report.setConfirmed(countMap.getOrDefault("CONFIRMED", 0L));
        report.setInProgress(countMap.getOrDefault("IN_PROGRESS", 0L));
        report.setCompleted(countMap.getOrDefault("COMPLETED", 0L));
        report.setCancelled(countMap.getOrDefault("CANCELLED", 0L));

        Map<String, Double> distribution = new LinkedHashMap<>();
        if (total > 0) {
            for (BookingStatus s : BookingStatus.values()) {
                long c = countMap.getOrDefault(s.name(), 0L);
                double pct = (c * 100.0) / total;
                distribution.put(s.name(), BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP).doubleValue());
            }
        }
        report.setStatusDistribution(distribution);

        List<Object[]> rows = bookingRepository.countBookingsByDayNative(from, to, workshopId);
        List<AdminReportTimelinePoint> points = new ArrayList<>();
        if (rows != null) {
            for (Object[] row : rows) {
                String dateStr = String.valueOf(row[0]);
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                BigDecimal amt = BigDecimal.ZERO;
                if (row.length > 2 && row[2] != null) {
                    amt = new BigDecimal(row[2].toString());
                }
                points.add(new AdminReportTimelinePoint(dateStr, count, amt));
            }
        }
        report.setTimeline(points);

        long days = 1;
        if (from != null && to != null) {
            days = Math.max(1, ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate()) + 1);
        } else if (from != null) {
            days = Math.max(1, ChronoUnit.DAYS.between(from.toLocalDate(), LocalDate.now()) + 1);
        } else {
            days = 30; // standard baseline divisor
        }
        double avg = (double) total / (double) days;
        report.setAverageBookingsPerDay(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());

        return report;
    }

    public AdminServiceCatalogReport getServiceCatalogReport() {
        AdminServiceCatalogReport report = new AdminServiceCatalogReport();

        long total = serviceCatalogRepository.count();
        long active = serviceCatalogRepository.countByIsActiveTrue();
        report.setTotalServiceCatalogItems(total);
        report.setActiveServices(active);
        report.setInactiveServices(Math.max(0, total - active));

        // Most requested services
        List<Object[]> requestedRows = serviceRequestItemRepository.findMostRequestedServices(PageRequest.of(0, 10));
        List<AdminServiceCatalogReport.TopServiceItem> topRequested = new ArrayList<>();
        if (requestedRows != null) {
            for (Object[] row : requestedRows) {
                String name = row[0] != null ? String.valueOf(row[0]) : "Service";
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                BigDecimal rev = row.length > 2 && row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                topRequested.add(new AdminServiceCatalogReport.TopServiceItem(name, "Requested", count, rev));
            }
        }
        report.setMostRequestedServices(topRequested);

        // Most booked services
        List<Object[]> bookedRows = bookingServiceRepository.findMostBookedServices(PageRequest.of(0, 10));
        List<AdminServiceCatalogReport.TopServiceItem> topBooked = new ArrayList<>();
        if (bookedRows != null) {
            for (Object[] row : bookedRows) {
                String name = row[0] != null ? String.valueOf(row[0]) : "Service";
                long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                BigDecimal rev = row.length > 2 && row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                topBooked.add(new AdminServiceCatalogReport.TopServiceItem(name, "Booked", count, rev));
            }
        }
        report.setMostBookedServices(topBooked);

        return report;
    }

    public Page<AdminReportActivityItem> getActivity(
            String search,
            String type,
            String status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        // If explicitly requesting SERVICE_REQUEST
        if ("SERVICE_REQUEST".equalsIgnoreCase(type)) {
            ServiceRequestStatus srStatus = null;
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                try {
                    srStatus = ServiceRequestStatus.valueOf(status.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            Page<ServiceRequest> srPage = serviceRequestRepository.findServiceRequestsWithFilter(
                    search, srStatus, null, null, from, to, pageable
            );
            List<AdminReportActivityItem> items = srPage.getContent().stream().map(this::mapServiceRequestToActivity).toList();
            return new PageImpl<>(items, pageable, srPage.getTotalElements());
        }

        // Default or BOOKING
        BookingStatus bStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                bStatus = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Page<Booking> bPage = bookingRepository.findBookingsWithFilter(
                search, bStatus, null, null, from, to, pageable
        );
        List<AdminReportActivityItem> items = bPage.getContent().stream().map(this::mapBookingToActivity).toList();
        return new PageImpl<>(items, pageable, bPage.getTotalElements());
    }

    private AdminReportActivityItem mapBookingToActivity(Booking b) {
        String custName = b.getUser() != null ? b.getUser().getName() : "Customer";
        String wkName = (b.getServiceRequest() != null && b.getServiceRequest().getAssignedWorkshop() != null)
                ? b.getServiceRequest().getAssignedWorkshop().getBusinessName()
                : "—";
        String sName = b.getServiceNameSnapshot() != null ? b.getServiceNameSnapshot() : "Standard Booking";

        return new AdminReportActivityItem(
                b.getId(),
                b.getCreatedAt(),
                "BOOKING",
                b.getBookingReference(),
                custName,
                wkName,
                sName,
                b.getStatus() != null ? b.getStatus().name() : "PENDING",
                b.getTotalAmount()
        );
    }

    private AdminReportActivityItem mapServiceRequestToActivity(ServiceRequest sr) {
        String custName = sr.getUser() != null ? sr.getUser().getName() : "Customer";
        String wkName = sr.getAssignedWorkshop() != null ? sr.getAssignedWorkshop().getBusinessName() : "—";

        return new AdminReportActivityItem(
                sr.getId(),
                sr.getCreatedAt(),
                "SERVICE_REQUEST",
                sr.getRequestReference(),
                custName,
                wkName,
                "Service Request",
                sr.getStatus() != null ? sr.getStatus().name() : "SUBMITTED",
                null
        );
    }

    public byte[] generateCsvExport(String reportType, LocalDateTime from, LocalDateTime to, String search) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        String type = reportType != null ? reportType.toUpperCase() : "OVERVIEW";

        switch (type) {
            case "CUSTOMERS" -> {
                writer.println("Metric,Value");
                AdminCustomerReport c = getCustomerReport(from, to);
                writer.println("Total Customers," + c.getTotalCustomers());
                writer.println("Active Customers," + c.getActiveCustomers());
                writer.println("Inactive / Suspended Customers," + c.getInactiveSuspendedCustomers());
                writer.println("New Customers in Period," + c.getNewCustomers());
                writer.println("Growth Rate (%)," + c.getGrowthRatePercentage() + "%");
                writer.println();
                writer.println("Date,Registrations");
                for (AdminReportTimelinePoint pt : c.getTimeline()) {
                    writer.println(pt.getDate() + "," + pt.getCount());
                }
            }
            case "WORKSHOPS" -> {
                writer.println("Metric,Value");
                AdminWorkshopReport w = getWorkshopReport(from, to);
                writer.println("Total Workshops," + w.getTotalWorkshops());
                writer.println("Pending Approvals," + w.getPendingApproval());
                writer.println("Verified Workshops," + w.getVerifiedWorkshops());
                writer.println("Rejected Workshops," + w.getRejectedWorkshops());
                writer.println("Active Workshops," + w.getActiveWorkshops());
                writer.println("Inactive Workshops," + w.getInactiveWorkshops());
                writer.println();
                writer.println("Date,Registrations");
                for (AdminReportTimelinePoint pt : w.getTimeline()) {
                    writer.println(pt.getDate() + "," + pt.getCount());
                }
            }
            case "BOOKINGS" -> {
                writer.println("Metric,Value");
                AdminBookingReport b = getBookingReport(from, to, null);
                writer.println("Total Bookings," + b.getTotalBookings());
                writer.println("Pending," + b.getPending());
                writer.println("Confirmed," + b.getConfirmed());
                writer.println("In Progress," + b.getInProgress());
                writer.println("Completed," + b.getCompleted());
                writer.println("Cancelled," + b.getCancelled());
                writer.println("Average Bookings Per Day," + b.getAverageBookingsPerDay());
                writer.println();
                writer.println("Date,Bookings,Revenue (INR)");
                for (AdminReportTimelinePoint pt : b.getTimeline()) {
                    writer.println(pt.getDate() + "," + pt.getCount() + "," + pt.getAmount());
                }
            }
            case "SERVICE_REQUESTS" -> {
                writer.println("Metric,Value");
                AdminServiceRequestReport sr = getServiceRequestReport(from, to, null);
                writer.println("Total Service Requests," + sr.getTotalRequests());
                writer.println("Submitted," + sr.getSubmitted());
                writer.println("Matched," + sr.getMatched());
                writer.println("Accepted," + sr.getAccepted());
                writer.println("In Progress," + sr.getInProgress());
                writer.println("Completed," + sr.getCompleted());
                writer.println("Cancelled," + sr.getCancelled());
                writer.println("Re-Matching," + sr.getReMatching());
                writer.println();
                writer.println("Date,Requests");
                for (AdminReportTimelinePoint pt : sr.getTimeline()) {
                    writer.println(pt.getDate() + "," + pt.getCount());
                }
            }
            case "ACTIVITY" -> {
                writer.println("Date,Type,Reference,Customer,Workshop,Service,Status,Amount");
                Page<AdminReportActivityItem> act = getActivity(search, null, null, from, to, 0, 1000);
                for (AdminReportActivityItem item : act.getContent()) {
                    writer.printf(
                            "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            item.getDate() != null ? item.getDate().toString() : "",
                            item.getType(),
                            item.getReference(),
                            item.getCustomerName() != null ? item.getCustomerName().replace("\"", "\"\"") : "",
                            item.getWorkshopName() != null ? item.getWorkshopName().replace("\"", "\"\"") : "",
                            item.getServiceName() != null ? item.getServiceName().replace("\"", "\"\"") : "",
                            item.getStatus(),
                            item.getAmount() != null ? item.getAmount().toString() : "0.00"
                    );
                }
            }
            default -> {
                // OVERVIEW
                AdminPlatformOverviewReport o = getPlatformOverview(from, to);
                writer.println("KPI Metric,Lifetime Total,Selected Period");
                writer.println("Total Customers," + o.getTotalCustomers() + "," + o.getPeriodCustomers());
                writer.println("Total Workshops," + o.getTotalWorkshops() + "," + o.getPeriodWorkshops());
                writer.println("Verified Workshops," + o.getVerifiedWorkshops() + ",—");
                writer.println("Pending Workshop Approvals," + o.getPendingWorkshopApprovals() + ",—");
                writer.println("Active Service Catalog Items," + o.getActiveServiceCatalogItems() + ",—");
                writer.println("Total Service Requests," + o.getTotalServiceRequests() + "," + o.getPeriodServiceRequests());
                writer.println("Total Bookings," + o.getTotalBookings() + "," + o.getPeriodBookings());
                writer.println("Completed Bookings," + o.getCompletedBookings() + "," + o.getPeriodCompletedBookings());
                writer.println("Cancelled Bookings," + o.getCancelledBookings() + "," + o.getPeriodCancelledBookings());
            }
        }

        writer.flush();
        return out.toByteArray();
    }
}
