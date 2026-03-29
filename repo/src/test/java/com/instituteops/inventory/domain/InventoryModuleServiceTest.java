package com.instituteops.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryModuleServiceTest {

    @Mock
    private InventoryUnitRepository inventoryUnitRepository;
    @Mock
    private UnitConversionRepository unitConversionRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private InventoryBatchRepository inventoryBatchRepository;
    @Mock
    private LossReasonCodeRepository lossReasonCodeRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private StockCountRepository stockCountRepository;
    @Mock
    private StockCountLineRepository stockCountLineRepository;
    @Mock
    private SystemAlertRepository systemAlertRepository;
    @Mock
    private ReplenishmentRecommendationRepository replenishmentRecommendationRepository;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;

    private InventoryModuleService service;

    @BeforeEach
    void setUp() {
        service = new InventoryModuleService(
            inventoryUnitRepository,
            unitConversionRepository,
            ingredientRepository,
            inventoryBatchRepository,
            lossReasonCodeRepository,
            inventoryTransactionRepository,
            stockCountRepository,
            stockCountLineRepository,
            systemAlertRepository,
            replenishmentRecommendationRepository,
            userIdentityService,
            userRepository
        );
    }

    @Test
    void issueFifo_lossRequiresReasonCode() {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setDefaultUnitId(1L);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));

        assertThatThrownBy(() -> service.issueFifo(new InventoryModuleService.IssueRequest(
            1L,
            "LOSS",
            new BigDecimal("2"),
            1L,
            null,
            "missing reason"
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Loss reason is mandatory");
    }

    @Test
    void issueFifo_consumesBatchesInExpiryOrder() {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setDefaultUnitId(1L);
        when(ingredientRepository.findById(1L)).thenReturn(Optional.of(ingredient));

        LossReasonCodeEntity lossReason = org.mockito.Mockito.mock(LossReasonCodeEntity.class);
        when(lossReason.getId()).thenReturn(1L);
        when(lossReasonCodeRepository.findByReasonCode("SPOILAGE")).thenReturn(Optional.of(lossReason));

        InventoryBatchEntity first = new InventoryBatchEntity();
        first.setQuantityAvailable(new BigDecimal("3.000"));
        first.setUnitCost(new BigDecimal("1.1000"));

        InventoryBatchEntity second = new InventoryBatchEntity();
        second.setQuantityAvailable(new BigDecimal("4.000"));
        second.setUnitCost(new BigDecimal("1.2000"));

        when(userIdentityService.resolveCurrentUserId()).thenReturn(Optional.of(9L));
        when(inventoryBatchRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(inventoryTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(inventoryBatchRepository.findByIngredientIdOrderByExpiresAtAscReceivedAtAsc(any())).thenReturn(List.of(first, second));

        List<InventoryTransactionEntity> txs = service.issueFifo(new InventoryModuleService.IssueRequest(
            1L,
            "LOSS",
            new BigDecimal("5"),
            1L,
            "SPOILAGE",
            "wastage"
        ));

        assertThat(txs).hasSize(2);
        assertThat(txs.get(0).getQuantity()).isEqualByComparingTo("3.000");
        assertThat(txs.get(1).getQuantity()).isEqualByComparingTo("2.000");
        assertThat(first.getQuantityAvailable()).isEqualByComparingTo("0.000");
        assertThat(first.getStatus()).isEqualTo("DEPLETED");
        assertThat(second.getQuantityAvailable()).isEqualByComparingTo("2.000");
        verify(inventoryBatchRepository).save(first);
        verify(inventoryBatchRepository).save(second);
    }
}
