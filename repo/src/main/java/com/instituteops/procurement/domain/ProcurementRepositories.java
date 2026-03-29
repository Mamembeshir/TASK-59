package com.instituteops.procurement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {
}

interface SupplierItemRepository extends JpaRepository<SupplierItemEntity, Long> {
}

interface SupplierPriceHistoryRepository extends JpaRepository<SupplierPriceHistoryEntity, Long> {
}

interface ProcurementRequestRepository extends JpaRepository<ProcurementRequestEntity, Long> {
}

interface ProcurementRequestLineRepository extends JpaRepository<ProcurementRequestLineEntity, Long> {
}

interface ProcurementApprovalRepository extends JpaRepository<ProcurementApprovalEntity, Long> {
}

interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {
}

interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, Long> {
}

interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptEntity, Long> {
}

interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLineEntity, Long> {
}
