package com.carservice.backend;

import com.carservice.backend.marketplace.dto.*;
import com.carservice.backend.marketplace.entity.*;
import com.carservice.backend.marketplace.enums.*;
import com.carservice.backend.marketplace.repository.*;
import com.carservice.backend.marketplace.service.WalletService;
import com.carservice.backend.security.jwt.JwtService;
import com.carservice.backend.user.entity.User;
import com.carservice.backend.user.enums.UserRole;
import com.carservice.backend.user.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class WorkshopWalletIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkshopRepository workshopRepository;

    @Autowired
    private WorkshopWalletRepository workshopWalletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private String adminToken;

    private User partnerUserA;
    private String partnerTokenA;
    private Workshop workshopA;

    private User partnerUserB;
    private String partnerTokenB;
    private Workshop workshopB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Admin
        adminUser = new User();
        adminUser.setName("System Admin");
        adminUser.setEmail("admin." + suffix + "@carservice.com");
        adminUser.setPhone("91" + (System.currentTimeMillis() % 100000000L));
        adminUser.setPassword(passwordEncoder.encode("Admin@123"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setIsActive(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        // 2. Partner A
        partnerUserA = new User();
        partnerUserA.setName("Partner Alpha");
        partnerUserA.setEmail("partner.alpha." + suffix + "@test.com");
        partnerUserA.setPhone("92" + (System.currentTimeMillis() % 100000000L));
        partnerUserA.setPassword(passwordEncoder.encode("Password@123"));
        partnerUserA.setRole(UserRole.PARTNER);
        partnerUserA.setIsActive(true);
        partnerUserA = userRepository.save(partnerUserA);
        partnerTokenA = jwtService.generateAccessToken(partnerUserA);

        workshopA = new Workshop(
                partnerUserA,
                "Alpha Motors " + suffix,
                partnerUserA.getPhone(),
                partnerUserA.getEmail(),
                "Connaught Place, New Delhi",
                "Delhi",
                "Delhi",
                "110001",
                new BigDecimal("28.6304"),
                new BigDecimal("77.2177"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopA = workshopRepository.save(workshopA);

        // 3. Partner B
        partnerUserB = new User();
        partnerUserB.setName("Partner Beta");
        partnerUserB.setEmail("partner.beta." + suffix + "@test.com");
        partnerUserB.setPhone("93" + (System.currentTimeMillis() % 100000000L));
        partnerUserB.setPassword(passwordEncoder.encode("Password@123"));
        partnerUserB.setRole(UserRole.PARTNER);
        partnerUserB.setIsActive(true);
        partnerUserB = userRepository.save(partnerUserB);
        partnerTokenB = jwtService.generateAccessToken(partnerUserB);

        workshopB = new Workshop(
                partnerUserB,
                "Beta Auto " + suffix,
                partnerUserB.getPhone(),
                partnerUserB.getEmail(),
                "South Extension, New Delhi",
                "Delhi",
                "Delhi",
                "110049",
                new BigDecimal("28.5700"),
                new BigDecimal("77.2200"),
                new BigDecimal("25.00"),
                WorkshopVerificationStatus.VERIFIED,
                true
        );
        workshopB = workshopRepository.save(workshopB);
    }

    // =========================================================================
    // 1 & 2. Initial Wallet Creation & Default Properties
    // =========================================================================
    @Test
    @DisplayName("1 & 2. New workshop wallet defaults to 0.00 balance, ACTIVE status, and INR currency")
    void initialWalletProperties() throws Exception {
        mockMvc.perform(get("/api/v1/partner/wallet")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workshopId").value(workshopA.getId()))
                .andExpect(jsonPath("$.data.workshopName").value(workshopA.getBusinessName()))
                .andExpect(jsonPath("$.data.balance").value(0.00))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(wallet.getBalance()));
        assertEquals("INR", wallet.getCurrency());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
    }

    // =========================================================================
    // 3 & 4. Credit Operations & Balance Progression
    // =========================================================================
    @Test
    @DisplayName("3 & 4. Credits increase balance accurately and accumulate consecutively with ledger records")
    void consecutiveCreditsAccumulate() {
        // First credit: ₹500.00
        WalletTransactionResponse tx1 = walletService.credit(
                workshopA,
                new BigDecimal("500.00"),
                WalletReferenceType.WALLET_TOPUP,
                "TOPUP-001",
                "KEY-TOPUP-001",
                "First topup"
        );

        assertNotNull(tx1.getId());
        assertEquals(WalletTransactionType.CREDIT, tx1.getType());
        assertEquals(0, new BigDecimal("500.00").compareTo(tx1.getAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(tx1.getBalanceBefore()));
        assertEquals(0, new BigDecimal("500.00").compareTo(tx1.getBalanceAfter()));

        // Second credit: ₹250.50
        WalletTransactionResponse tx2 = walletService.credit(
                workshopA,
                new BigDecimal("250.50"),
                WalletReferenceType.WALLET_TOPUP,
                "TOPUP-002",
                "KEY-TOPUP-002",
                "Second topup"
        );

        assertEquals(0, new BigDecimal("250.50").compareTo(tx2.getAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(tx2.getBalanceBefore()));
        assertEquals(0, new BigDecimal("750.50").compareTo(tx2.getBalanceAfter()));

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("750.50").compareTo(wallet.getBalance()));
    }

    // =========================================================================
    // 5. Debit Operation & Balance Reduction
    // =========================================================================
    @Test
    @DisplayName("5. Debit decreases balance accurately and records ledger before and after balance")
    void debitDecreasesBalanceAccurately() {
        walletService.credit(workshopA, new BigDecimal("750.50"), WalletReferenceType.WALLET_TOPUP, "TOPUP-1", null, "Initial credit");

        WalletTransactionResponse debitTx = walletService.debit(
                workshopA,
                new BigDecimal("99.00"),
                WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                "OPP-101",
                "KEY-DEB-001",
                "Lead acceptance fee"
        );

        assertEquals(WalletTransactionType.DEBIT, debitTx.getType());
        assertEquals(WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, debitTx.getReferenceType());
        assertEquals(0, new BigDecimal("99.00").compareTo(debitTx.getAmount()));
        assertEquals(0, new BigDecimal("750.50").compareTo(debitTx.getBalanceBefore()));
        assertEquals(0, new BigDecimal("651.50").compareTo(debitTx.getBalanceAfter()));

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("651.50").compareTo(wallet.getBalance()));
    }

    // =========================================================================
    // 6. Insufficient Balance Prevents Debit
    // =========================================================================
    @Test
    @DisplayName("6. Insufficient wallet balance prevents debit and leaves balance intact")
    void insufficientBalancePreventsDebit() {
        walletService.credit(workshopA, new BigDecimal("50.00"), WalletReferenceType.WALLET_TOPUP, "TOPUP-50", null, "Credit 50");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                walletService.debit(
                        workshopA,
                        new BigDecimal("99.00"),
                        WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                        "OPP-99",
                        null,
                        "Attempt debit"
                )
        );

        assertTrue(ex.getMessage().contains("Insufficient wallet balance"));

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(wallet.getBalance()));
    }

    // =========================================================================
    // 7 & 8. Zero or Negative Amount Rejections
    // =========================================================================
    @Test
    @DisplayName("7 & 8. Zero or negative amounts are rejected for both credit and debit")
    void zeroOrNegativeAmountsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                walletService.credit(workshopA, BigDecimal.ZERO, WalletReferenceType.WALLET_TOPUP, "REF-0", null, "Zero credit")
        );

        assertThrows(IllegalArgumentException.class, () ->
                walletService.credit(workshopA, new BigDecimal("-100.00"), WalletReferenceType.WALLET_TOPUP, "REF-NEG", null, "Neg credit")
        );

        assertThrows(IllegalArgumentException.class, () ->
                walletService.debit(workshopA, BigDecimal.ZERO, WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, "REF-0", null, "Zero debit")
        );

        assertThrows(IllegalArgumentException.class, () ->
                walletService.debit(workshopA, new BigDecimal("-50.00"), WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, "REF-NEG", null, "Neg debit")
        );
    }

    // =========================================================================
    // 9 & 10. Idempotency Key Guards
    // =========================================================================
    @Test
    @DisplayName("9 & 10. Repeated calls with identical idempotency key return original transaction without double debit/credit")
    void idempotencyProtection() {
        walletService.credit(workshopA, new BigDecimal("1000.00"), WalletReferenceType.WALLET_TOPUP, "INIT", null, "Initial credit");

        String debitKey = "IDEMP-DEBIT-UNIQUE-KEY-001";

        WalletTransactionResponse debit1 = walletService.debit(
                workshopA,
                new BigDecimal("99.00"),
                WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                "OPP-201",
                debitKey,
                "Lead fee"
        );

        // Repeated debit with same key
        WalletTransactionResponse debit2 = walletService.debit(
                workshopA,
                new BigDecimal("99.00"),
                WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                "OPP-201",
                debitKey,
                "Lead fee"
        );

        assertEquals(debit1.getId(), debit2.getId());
        assertEquals(0, new BigDecimal("901.00").compareTo(debit2.getBalanceAfter()));

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("901.00").compareTo(wallet.getBalance()));

        // Repeated credit with same key
        String creditKey = "IDEMP-CREDIT-UNIQUE-KEY-002";
        WalletTransactionResponse cred1 = walletService.credit(
                workshopA,
                new BigDecimal("200.00"),
                WalletReferenceType.WALLET_TOPUP,
                "TOP-999",
                creditKey,
                "Topup"
        );

        WalletTransactionResponse cred2 = walletService.credit(
                workshopA,
                new BigDecimal("200.00"),
                WalletReferenceType.WALLET_TOPUP,
                "TOP-999",
                creditKey,
                "Topup"
        );

        assertEquals(cred1.getId(), cred2.getId());
        assertEquals(0, new BigDecimal("1101.00").compareTo(cred2.getBalanceAfter()));

        WorkshopWallet walletAfterCredit = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1101.00").compareTo(walletAfterCredit.getBalance()));
    }

    // =========================================================================
    // 11. Duplicate Refund Prevention
    // =========================================================================
    @Test
    @DisplayName("11. Refund credit is idempotent by referenceId and prevents double crediting")
    void duplicateRefundCreditPrevented() {
        walletService.credit(workshopA, new BigDecimal("500.00"), WalletReferenceType.WALLET_TOPUP, "TOP", null, "Initial");

        String refundRef = "REFUND-PYMT-12345";

        WalletTransactionResponse ref1 = walletService.credit(
                workshopA,
                new BigDecimal("99.00"),
                WalletReferenceType.OPPORTUNITY_REFUND,
                refundRef,
                null,
                "Refund for race condition"
        );

        // Attempt second refund with same reference ID
        WalletTransactionResponse ref2 = walletService.credit(
                workshopA,
                new BigDecimal("99.00"),
                WalletReferenceType.OPPORTUNITY_REFUND,
                refundRef,
                null,
                "Refund for race condition"
        );

        assertEquals(ref1.getId(), ref2.getId());

        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("599.00").compareTo(wallet.getBalance()));
    }

    // =========================================================================
    // 12 & 13. Top-up Preparation Intent API
    // =========================================================================
    @Test
    @DisplayName("12 & 13. Top-up initiate endpoint enforces limits (₹100 - ₹50,000) and returns intent without mutating balance")
    void topupInitiateEndpoint() throws Exception {
        // Below min ₹100
        TopupInitiateRequest lowReq = new TopupInitiateRequest(new BigDecimal("50.00"), "Too low");
        mockMvc.perform(post("/api/v1/partner/wallet/topup/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowReq)))
                .andExpect(status().isBadRequest());

        // Above max ₹50,000
        TopupInitiateRequest highReq = new TopupInitiateRequest(new BigDecimal("60000.00"), "Too high");
        mockMvc.perform(post("/api/v1/partner/wallet/topup/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highReq)))
                .andExpect(status().isBadRequest());

        // Valid ₹1,000.00
        TopupInitiateRequest validReq = new TopupInitiateRequest(new BigDecimal("1000.00"), "Recharge balance");
        mockMvc.perform(post("/api/v1/partner/wallet/topup/initiate")
                        .header("Authorization", "Bearer " + partnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intentId", startsWith("TOPUP-INTENT-")))
                .andExpect(jsonPath("$.data.amount").value(1000.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.currency").value("INR"));

        // Verify wallet balance is STILL 0.00 (not credited until gateway verification in Step 18E)
        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }

    // =========================================================================
    // 14, 15 & 16. Partner Isolation
    // =========================================================================
    @Test
    @DisplayName("14, 15 & 16. Partner accounts are strictly isolated: Partner A cannot view or access Partner B's wallet or ledger")
    void partnerIsolation() throws Exception {
        // Setup balance and transaction for Workshop B
        WalletTransactionResponse txB = walletService.credit(
                workshopB,
                new BigDecimal("800.00"),
                WalletReferenceType.WALLET_TOPUP,
                "TOP-B",
                null,
                "Beta Topup"
        );

        // Partner A requests their own wallet: receives Workshop A, NOT Workshop B
        mockMvc.perform(get("/api/v1/partner/wallet")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workshopId").value(workshopA.getId()))
                .andExpect(jsonPath("$.data.workshopId").value(not(workshopB.getId())));

        // Partner A requests their transactions: sees 0 transactions (not txB)
        mockMvc.perform(get("/api/v1/partner/wallet/transactions")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Partner A requests Partner B's transaction by ID: receives 404
        mockMvc.perform(get("/api/v1/partner/wallet/transactions/" + txB.getId())
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isNotFound());

        // Partner B requests their own transaction by ID: receives 200 OK
        mockMvc.perform(get("/api/v1/partner/wallet/transactions/" + txB.getId())
                        .header("Authorization", "Bearer " + partnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(txB.getId()))
                .andExpect(jsonPath("$.data.workshopId").value(workshopB.getId()));
    }

    // =========================================================================
    // 17 & 18. Paginated Ledger & Type Filtering
    // =========================================================================
    @Test
    @DisplayName("17 & 18. Transactions endpoint supports pagination and type filtering")
    void paginatedLedgerAndFiltering() throws Exception {
        walletService.credit(workshopA, new BigDecimal("100.00"), WalletReferenceType.WALLET_TOPUP, "C1", null, "Credit 1");
        walletService.credit(workshopA, new BigDecimal("200.00"), WalletReferenceType.WALLET_TOPUP, "C2", null, "Credit 2");
        walletService.credit(workshopA, new BigDecimal("300.00"), WalletReferenceType.WALLET_TOPUP, "C3", null, "Credit 3");
        walletService.debit(workshopA, new BigDecimal("50.00"), WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, "D1", null, "Debit 1");
        walletService.debit(workshopA, new BigDecimal("40.00"), WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, "D2", null, "Debit 2");

        // Paginated query: page 0, size 2
        mockMvc.perform(get("/api/v1/partner/wallet/transactions?page=0&size=2")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // Filter: CREDIT only
        mockMvc.perform(get("/api/v1/partner/wallet/transactions?type=CREDIT")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.data[1].type").value("CREDIT"))
                .andExpect(jsonPath("$.data[2].type").value("CREDIT"));

        // Filter: DEBIT only
        mockMvc.perform(get("/api/v1/partner/wallet/transactions?type=DEBIT")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.data[1].type").value("DEBIT"));
    }

    // =========================================================================
    // 19. Multi-threaded Concurrency Test (Pessimistic Locking & Versioning)
    // =========================================================================
    @Test
    @DisplayName("19. Concurrent debits on exact balance: exactly 1 succeeds, exactly 1 fails, balance ₹0.00")
    void concurrentDebitRaceCondition() throws Exception {
        // Setup exact balance ₹99.00
        walletService.credit(workshopA, new BigDecimal("99.00"), WalletReferenceType.WALLET_TOPUP, "EXACT-99", null, "Topup 99");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for simultaneous trigger
                    walletService.debit(
                            workshopA,
                            new BigDecimal("99.00"),
                            WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE,
                            "OPP-CONC-" + index,
                            "KEY-CONC-" + index,
                            "Concurrent debit test"
                    );
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrency test timed out");
        assertEquals(1, successCount.get(), "Expected exactly 1 debit to succeed");
        assertEquals(1, failureCount.get(), "Expected exactly 1 debit to fail due to insufficient balance");

        // Final balance must be exactly ₹0.00 and NEVER negative
        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()), "Final balance must be exactly 0.00");

        // Exactly 1 DEBIT transaction recorded
        List<WalletTransaction> debitTxs = walletTransactionRepository
                .findByWorkshopIdAndTypeOrderByCreatedAtDesc(workshopA.getId(), WalletTransactionType.DEBIT);
        assertEquals(1, debitTxs.size());
    }

    // =========================================================================
    // 20. Suspended Wallet Blocks Operations
    // =========================================================================
    @Test
    @DisplayName("20. Suspended wallet blocks credit and debit attempts")
    void suspendedWalletBlocksOperations() {
        WorkshopWallet wallet = workshopWalletRepository.findByWorkshopId(workshopA.getId())
                .orElseGet(() -> workshopWalletRepository.save(new WorkshopWallet(workshopA, BigDecimal.ZERO, "INR", WalletStatus.ACTIVE)));

        wallet.setStatus(WalletStatus.SUSPENDED);
        workshopWalletRepository.save(wallet);

        assertThrows(IllegalStateException.class, () ->
                walletService.credit(workshopA, new BigDecimal("100.00"), WalletReferenceType.WALLET_TOPUP, "TOP-S", null, "Credit suspended")
        );

        assertThrows(IllegalStateException.class, () ->
                walletService.debit(workshopA, new BigDecimal("50.00"), WalletReferenceType.OPPORTUNITY_ACCEPTANCE_FEE, "DEB-S", null, "Debit suspended")
        );
    }

    // =========================================================================
    // 21, 22 & 23. Admin Operations (Audit & Controlled Adjustments)
    // =========================================================================
    @Test
    @DisplayName("21. Admin can list all workshop wallets, but partner is forbidden (403)")
    void adminCanViewAllWallets() throws Exception {
        // Partner A forbidden
        mockMvc.perform(get("/api/v1/admin/wallets")
                        .header("Authorization", "Bearer " + partnerTokenA))
                .andExpect(status().isForbidden());

        // Admin allowed
        mockMvc.perform(get("/api/v1/admin/wallets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("22. Admin can view specific workshop wallet and audit transactions")
    void adminCanViewWorkshopTransactions() throws Exception {
        walletService.credit(workshopA, new BigDecimal("350.00"), WalletReferenceType.WALLET_TOPUP, "TOP-ADM-TEST", null, "Topup for admin test");

        mockMvc.perform(get("/api/v1/admin/wallets/" + workshopA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workshopId").value(workshopA.getId()))
                .andExpect(jsonPath("$.data.balance").value(350.00));

        mockMvc.perform(get("/api/v1/admin/wallets/" + workshopA.getId() + "/transactions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("23. Admin can perform controlled credit and debit adjustments with mandatory audit reasons")
    void adminAdjustWallet() throws Exception {
        walletService.credit(workshopA, new BigDecimal("100.00"), WalletReferenceType.WALLET_TOPUP, "INIT", null, "Init");

        // 1. Admin Credit Adjustment
        AdminWalletAdjustmentRequest creditAdj = new AdminWalletAdjustmentRequest(
                workshopA.getId(),
                WalletTransactionType.CREDIT,
                new BigDecimal("250.00"),
                "Compensation for platform delay",
                "TICKET-8891"
        );

        mockMvc.perform(post("/api/v1/admin/wallets/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditAdj)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("CREDIT"))
                .andExpect(jsonPath("$.data.referenceType").value("ADMIN_CREDIT"))
                .andExpect(jsonPath("$.data.amount").value(250.00))
                .andExpect(jsonPath("$.data.balanceBefore").value(100.00))
                .andExpect(jsonPath("$.data.balanceAfter").value(350.00))
                .andExpect(jsonPath("$.data.description", containsString("admin.")));

        // 2. Admin Debit Adjustment
        AdminWalletAdjustmentRequest debitAdj = new AdminWalletAdjustmentRequest(
                workshopA.getId(),
                WalletTransactionType.DEBIT,
                new BigDecimal("50.00"),
                "Correction of incorrect promotional credit",
                "TICKET-8892"
        );

        mockMvc.perform(post("/api/v1/admin/wallets/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debitAdj)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("DEBIT"))
                .andExpect(jsonPath("$.data.referenceType").value("ADMIN_DEBIT"))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.balanceBefore").value(350.00))
                .andExpect(jsonPath("$.data.balanceAfter").value(300.00));

        // 3. Adjustment without reason is rejected (400 Bad Request)
        AdminWalletAdjustmentRequest noReasonAdj = new AdminWalletAdjustmentRequest(
                workshopA.getId(),
                WalletTransactionType.DEBIT,
                new BigDecimal("50.00"),
                "", // Blank reason
                "TICKET-EMPTY"
        );

        mockMvc.perform(post("/api/v1/admin/wallets/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noReasonAdj)))
                .andExpect(status().isBadRequest());
    }
}
