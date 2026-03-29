package com.instituteops.procurement.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    Optional<SupplierEntity> findBySupplierCode(String supplierCode);
}

interface SupplierItemRepository extends JpaRepository<SupplierItemEntity, Long> {

    List<SupplierItemEntity> findBySupplierId(Long supplierId);

    Optional<SupplierItemEntity> findBySupplierIdAndIngredientId(Long supplierId, Long ingredientId);
}

interface SupplierPriceHistoryRepository extends JpaRepository<SupplierPriceHistoryEntity, Long> {

    List<SupplierPriceHistoryEntity> findBySupplierItemIdOrderByEffectiveFromDesc(Long supplierItemId);

    @Query("""
        select p from SupplierPriceHistoryEntity p
        where p.supplierItemId = :itemId
          and p.effectiveFrom <= :today
          and (p.effectiveTo is null or p.effectiveTo >= :today)
        order by p.effectiveFrom desc
        """)
    List<SupplierPriceHistoryEntity> findActivePriceHistory(@Param("itemId") Long itemId, @Param("today") LocalDate today);
}

interface ProcurementRequestRepository extends JpaRepository<ProcurementRequestEntity, Long> {

    Optional<ProcurementRequestEntity> findByRequestNo(String requestNo);

    List<ProcurementRequestEntity> findTop100ByOrderByRequestedAtDesc();
}

interface ProcurementRequestLineRepository extends JpaRepository<ProcurementRequestLineEntity, Long> {

    List<ProcurementRequestLineEntity> findByRequestId(Long requestId);
}

interface ProcurementApprovalRepository extends JpaRepository<ProcurementApprovalEntity, Long> {

    List<ProcurementApprovalEntity> findByRequestIdOrderByDecidedAtDesc(Long requestId);
}

interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    Optional<PurchaseOrderEntity> findByRequestId(Long requestId);

    List<PurchaseOrderEntity> findTop100ByOrderByOrderedAtDesc();
}

interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, Long> {

    List<PurchaseOrderLineEntity> findByPurchaseOrderId(Long purchaseOrderId);
}

interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptEntity, Long> {

    List<GoodsReceiptEntity> findTop100ByOrderByReceivedAtDesc();

    List<GoodsReceiptEntity> findByPurchaseOrderId(Long purchaseOrderId);
}

interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLineEntity, Long> {

    List<GoodsReceiptLineEntity> findByGoodsReceiptId(Long goodsReceiptId);
}
