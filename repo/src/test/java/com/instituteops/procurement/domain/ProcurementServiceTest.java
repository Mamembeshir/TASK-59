package com.instituteops.procurement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierItemRepository supplierItemRepository;
    @Mock
    private SupplierPriceHistoryRepository supplierPriceHistoryRepository;
    @Mock
    private ProcurementRequestRepository procurementRequestRepository;
    @Mock
    private ProcurementRequestLineRepository procurementRequestLineRepository;
    @Mock
    private ProcurementApprovalRepository procurementApprovalRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderLineRepository purchaseOrderLineRepository;
    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;
    @Mock
    private GoodsReceiptLineRepository goodsReceiptLineRepository;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private ProcurementService service;

    @BeforeEach
    void setUp() {
        service = new ProcurementService(
            supplierRepository,
            supplierItemRepository,
            supplierPriceHistoryRepository,
            procurementRequestRepository,
            procurementRequestLineRepository,
            procurementApprovalRepository,
            purchaseOrderRepository,
            purchaseOrderLineRepository,
            goodsReceiptRepository,
            goodsReceiptLineRepository,
            userIdentityService,
            userRepository,
            jdbcTemplate
        );
    }

    @Test
    void approve_persistsDecisionAndUpdatesRequestStatus() {
        ProcurementRequestEntity request = new ProcurementRequestEntity();
        request.setStatus("SUBMITTED");
        when(procurementRequestRepository.findById(99L)).thenReturn(Optional.of(request));
        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(5L));
        when(procurementApprovalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProcurementApprovalEntity saved = service.approve(99L, "approved", "ok to buy");

        assertThat(saved.getDecision()).isEqualTo("APPROVED");
        assertThat(request.getStatus()).isEqualTo("APPROVED");
        verify(procurementRequestRepository).save(request);
    }

    @Test
    void createPurchaseOrder_requiresApprovedRequest() {
        ProcurementRequestEntity request = new ProcurementRequestEntity();
        request.setStatus("SUBMITTED");
        when(procurementRequestRepository.findById(33L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.createPurchaseOrder(33L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be APPROVED");
    }
}
