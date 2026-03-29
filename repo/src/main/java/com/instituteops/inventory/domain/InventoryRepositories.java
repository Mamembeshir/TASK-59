package com.instituteops.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryUnitRepository extends JpaRepository<InventoryUnitEntity, Long> {
}

interface UnitConversionRepository extends JpaRepository<UnitConversionEntity, Long> {
}

interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {
}

interface InventoryBatchRepository extends JpaRepository<InventoryBatchEntity, Long> {
}

interface LossReasonCodeRepository extends JpaRepository<LossReasonCodeEntity, Long> {
}

interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, Long> {
}

interface StockCountRepository extends JpaRepository<StockCountEntity, Long> {
}

interface StockCountLineRepository extends JpaRepository<StockCountLineEntity, Long> {
}

interface SystemAlertRepository extends JpaRepository<SystemAlertEntity, Long> {
}

interface ReplenishmentRecommendationRepository extends JpaRepository<ReplenishmentRecommendationEntity, Long> {
}
