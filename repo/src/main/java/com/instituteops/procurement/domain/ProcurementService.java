package com.instituteops.procurement.domain;

import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcurementService {

    private final SupplierRepository supplierRepository;
    private final SupplierItemRepository supplierItemRepository;
    private final SupplierPriceHistoryRepository supplierPriceHistoryRepository;
    private final ProcurementRequestRepository procurementRequestRepository;
    private final ProcurementRequestLineRepository procurementRequestLineRepository;
    private final ProcurementApprovalRepository procurementApprovalRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptLineRepository goodsReceiptLineRepository;
    private final UserIdentityService userIdentityService;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ProcurementUnitConversionService unitConversionService;

    public ProcurementService(
        SupplierRepository supplierRepository,
        SupplierItemRepository supplierItemRepository,
        SupplierPriceHistoryRepository supplierPriceHistoryRepository,
        ProcurementRequestRepository procurementRequestRepository,
        ProcurementRequestLineRepository procurementRequestLineRepository,
        ProcurementApprovalRepository procurementApprovalRepository,
        PurchaseOrderRepository purchaseOrderRepository,
        PurchaseOrderLineRepository purchaseOrderLineRepository,
        GoodsReceiptRepository goodsReceiptRepository,
        GoodsReceiptLineRepository goodsReceiptLineRepository,
        UserIdentityService userIdentityService,
        UserRepository userRepository,
        JdbcTemplate jdbcTemplate,
        ProcurementUnitConversionService unitConversionService
    ) {
        this.supplierRepository = supplierRepository;
        this.supplierItemRepository = supplierItemRepository;
        this.supplierPriceHistoryRepository = supplierPriceHistoryRepository;
        this.procurementRequestRepository = procurementRequestRepository;
        this.procurementRequestLineRepository = procurementRequestLineRepository;
        this.procurementApprovalRepository = procurementApprovalRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptLineRepository = goodsReceiptLineRepository;
        this.userIdentityService = userIdentityService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.unitConversionService = unitConversionService;
    }

    public List<SupplierEntity> suppliers() {
        return supplierRepository.findAll();
    }

    public List<SupplierItemEntity> supplierItems() {
        return supplierItemRepository.findAll();
    }

    public List<SupplierPriceHistoryEntity> priceHistory() {
        return supplierPriceHistoryRepository.findAll();
    }

    public List<ProcurementRequestEntity> requests() {
        return procurementRequestRepository.findTop100ByOrderByRequestedAtDesc();
    }

    public List<PurchaseOrderEntity> purchaseOrders() {
        return purchaseOrderRepository.findTop100ByOrderByOrderedAtDesc();
    }

    public List<GoodsReceiptEntity> receipts() {
        return goodsReceiptRepository.findTop100ByOrderByReceivedAtDesc();
    }

    @Transactional
    public SupplierEntity createSupplier(String supplierCode, String supplierName, String contactName) {
        if (!StringUtils.hasText(supplierCode) || !StringUtils.hasText(supplierName)) {
            throw new IllegalArgumentException("supplierCode and supplierName are required");
        }
        SupplierEntity supplier = new SupplierEntity();
        supplier.setSupplierCode(supplierCode.trim().toUpperCase(Locale.ROOT));
        supplier.setSupplierName(supplierName.trim());
        supplier.setContactName(contactName);
        supplier.setActive(true);
        return supplierRepository.save(supplier);
    }

    @Transactional
    public SupplierItemEntity createSupplierItem(Long supplierId, Long ingredientId, String supplierSku, BigDecimal packSize, Long packUnitId, boolean preferred) {
        if (supplierId == null || ingredientId == null || packSize == null || packSize.compareTo(BigDecimal.ZERO) <= 0 || packUnitId == null) {
            throw new IllegalArgumentException("supplierId, ingredientId, packSize, packUnitId are required");
        }
        SupplierItemEntity item = supplierItemRepository.findBySupplierIdAndIngredientId(supplierId, ingredientId).orElseGet(SupplierItemEntity::new);
        item.setSupplierId(supplierId);
        item.setIngredientId(ingredientId);
        item.setSupplierSku(supplierSku);
        item.setPackSize(packSize);
        item.setPackUnitId(packUnitId);
        item.setPreferred(preferred);
        item.setActive(true);
        return supplierItemRepository.save(item);
    }

    @Transactional
    public SupplierPriceHistoryEntity addPriceHistory(Long supplierItemId, BigDecimal price, String currencyCode, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (supplierItemId == null || price == null || price.compareTo(BigDecimal.ZERO) <= 0 || effectiveFrom == null) {
            throw new IllegalArgumentException("supplierItemId, price, effectiveFrom are required");
        }
        SupplierPriceHistoryEntity history = new SupplierPriceHistoryEntity();
        history.setSupplierItemId(supplierItemId);
        history.setPrice(price);
        history.setCurrencyCode(StringUtils.hasText(currencyCode) ? currencyCode.toUpperCase(Locale.ROOT) : "USD");
        history.setEffectiveFrom(effectiveFrom);
        history.setEffectiveTo(effectiveTo);
        return supplierPriceHistoryRepository.save(history);
    }

    @Transactional
    public ProcurementRequestEntity createRequest(String justification, LocalDate neededBy) {
        ProcurementRequestEntity request = new ProcurementRequestEntity();
        request.setRequestNo("PR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        request.setRequestedBy(currentUserId());
        request.setRequestedAt(LocalDateTime.now());
        request.setStatus("DRAFT");
        request.setJustification(justification);
        request.setNeededBy(neededBy);
        return procurementRequestRepository.save(request);
    }

    @Transactional
    public ProcurementRequestLineEntity addRequestLine(Long requestId, Long ingredientId, BigDecimal qty, Long unitId, BigDecimal estimatedUnitPrice, String note) {
        ProcurementRequestEntity request = procurementRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (request.getStatus().equals("APPROVED") || request.getStatus().equals("ORDERED") || request.getStatus().equals("RECEIVED") || request.getStatus().equals("CLOSED")) {
            throw new IllegalArgumentException("Cannot edit request in current status");
        }
        ProcurementRequestLineEntity line = new ProcurementRequestLineEntity();
        line.setRequestId(requestId);
        line.setIngredientId(ingredientId);
        line.setRequestedQty(qty);
        line.setUnitId(unitId);
        line.setEstimatedUnitPrice(estimatedUnitPrice);
        line.setNote(note);
        request.setStatus("SUBMITTED");
        procurementRequestRepository.save(request);
        return procurementRequestLineRepository.save(line);
    }

    @Transactional
    public ProcurementApprovalEntity approve(Long requestId, String decision, String note) {
        ProcurementRequestEntity request = procurementRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        String normalized = StringUtils.hasText(decision) ? decision.trim().toUpperCase(Locale.ROOT) : "";
        if (!List.of("APPROVED", "REJECTED").contains(normalized)) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }
        ProcurementApprovalEntity approval = new ProcurementApprovalEntity();
        approval.setRequestId(requestId);
        approval.setApproverUserId(currentUserId());
        approval.setDecision(normalized);
        approval.setDecisionNote(note);
        approval.setDecidedAt(LocalDateTime.now());
        procurementApprovalRepository.save(approval);

        request.setStatus(normalized);
        procurementRequestRepository.save(request);
        return approval;
    }

    @Transactional
    public PurchaseOrderEntity createPurchaseOrder(Long requestId, Long supplierId) {
        ProcurementRequestEntity request = procurementRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!"APPROVED".equals(request.getStatus())) {
            throw new IllegalArgumentException("Request must be APPROVED before creating PO");
        }
        PurchaseOrderEntity po = new PurchaseOrderEntity();
        po.setPoNo("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        po.setRequestId(requestId);
        po.setSupplierId(supplierId);
        po.setOrderedBy(currentUserId());
        po.setOrderedAt(LocalDateTime.now());
        po.setStatus("OPEN");
        po.setCurrencyCode("USD");
        po.setTotalAmount(BigDecimal.ZERO);
        PurchaseOrderEntity saved = purchaseOrderRepository.save(po);

        BigDecimal total = BigDecimal.ZERO;
        for (ProcurementRequestLineEntity reqLine : procurementRequestLineRepository.findByRequestId(requestId)) {
            BigDecimal price = resolveSupplierPrice(supplierId, reqLine.getIngredientId()).orElse(
                reqLine.getEstimatedUnitPrice() == null ? BigDecimal.ONE : reqLine.getEstimatedUnitPrice()
            );
            SupplierItemEntity supplierItem = supplierItemRepository.findBySupplierIdAndIngredientId(supplierId, reqLine.getIngredientId()).orElse(null);
            BigDecimal orderedQty = reqLine.getRequestedQty().setScale(3, RoundingMode.HALF_UP);
            Long unitId = reqLine.getUnitId();
            BigDecimal unitPrice = price;
            if (supplierItem != null && supplierItem.getPackSize() != null && supplierItem.getPackUnitId() != null
                && supplierItem.getPackSize().compareTo(BigDecimal.ZERO) > 0) {
                orderedQty = unitConversionService.convertQuantity(reqLine.getRequestedQty(), reqLine.getUnitId(), supplierItem.getPackUnitId());
                unitId = supplierItem.getPackUnitId();
                unitPrice = price.divide(supplierItem.getPackSize(), 4, RoundingMode.HALF_UP);
            }

            PurchaseOrderLineEntity line = new PurchaseOrderLineEntity();
            line.setPurchaseOrderId(saved.getId());
            line.setIngredientId(reqLine.getIngredientId());
            line.setOrderedQty(orderedQty);
            line.setReceivedQty(BigDecimal.ZERO);
            line.setUnitId(unitId);
            line.setUnitPrice(unitPrice);
            BigDecimal lineAmount = unitPrice.multiply(orderedQty).setScale(2, RoundingMode.HALF_UP);
            line.setLineAmount(lineAmount);
            purchaseOrderLineRepository.save(line);
            total = total.add(lineAmount);
        }
        saved.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        purchaseOrderRepository.save(saved);

        request.setStatus("ORDERED");
        procurementRequestRepository.save(request);
        return saved;
    }

    @Transactional
    public GoodsReceiptEntity receive(ReceiveRequest receiveRequest) {
        PurchaseOrderEntity po = purchaseOrderRepository.findById(receiveRequest.purchaseOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        GoodsReceiptEntity receipt = new GoodsReceiptEntity();
        receipt.setReceiptNo("GR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        receipt.setPurchaseOrderId(po.getId());
        receipt.setReceivedBy(currentUserId());
        receipt.setReceivedAt(LocalDateTime.now());
        receipt.setStatus("POSTED");
        receipt.setNote(receiveRequest.note());
        GoodsReceiptEntity savedReceipt = goodsReceiptRepository.save(receipt);

        BigDecimal anyPending = BigDecimal.ZERO;
        for (ReceiveLine line : receiveRequest.lines()) {
            PurchaseOrderLineEntity poLine = purchaseOrderLineRepository.findById(line.purchaseOrderLineId())
                .orElseThrow(() -> new IllegalArgumentException("PO line not found"));
            if (!poLine.getPurchaseOrderId().equals(po.getId())) {
                throw new IllegalArgumentException("PO line does not belong to selected PO");
            }
            BigDecimal accepted = line.acceptedQty();
            BigDecimal rejected = line.rejectedQty() == null ? BigDecimal.ZERO : line.rejectedQty();
            BigDecimal receivedQty = accepted.add(rejected);

            GoodsReceiptLineEntity grLine = new GoodsReceiptLineEntity();
            grLine.setGoodsReceiptId(savedReceipt.getId());
            grLine.setPurchaseOrderLineId(poLine.getId());
            grLine.setBatchNo(line.batchNo());
            grLine.setReceivedQty(receivedQty);
            grLine.setAcceptedQty(accepted);
            grLine.setRejectedQty(rejected);
            grLine.setUnitId(poLine.getUnitId());
            grLine.setUnitCost(poLine.getUnitPrice());
            grLine.setExpiresAt(line.expiresAt());
            goodsReceiptLineRepository.save(grLine);

            poLine.setReceivedQty(poLine.getReceivedQty().add(accepted));
            purchaseOrderLineRepository.save(poLine);

            BigDecimal remaining = poLine.getOrderedQty().subtract(poLine.getReceivedQty());
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                anyPending = anyPending.add(remaining);
            }

            insertInventoryBatchAndTransaction(po, poLine, line, accepted);
        }

        po.setStatus(anyPending.compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_RECEIVED" : "RECEIVED");
        purchaseOrderRepository.save(po);

        ProcurementRequestEntity request = procurementRequestRepository.findById(po.getRequestId())
            .orElseThrow(() -> new IllegalArgumentException("Related request not found"));
        request.setStatus(anyPending.compareTo(BigDecimal.ZERO) > 0 ? "RECEIVED" : "CLOSED");
        procurementRequestRepository.save(request);
        return savedReceipt;
    }

    public void refreshAlertsAndRecommendations() {
        List<Map<String, Object>> ingredients = jdbcTemplate.queryForList("SELECT id, default_unit_id FROM ingredients WHERE active = true");
        for (Map<String, Object> ingredient : ingredients) {
            Long ingredientId = ((Number) ingredient.get("id")).longValue();
            Long defaultUnitId = ((Number) ingredient.get("default_unit_id")).longValue();

            BigDecimal stock = queryDecimal("SELECT COALESCE(SUM(quantity_available),0) FROM inventory_batches WHERE ingredient_id = ?", ingredientId);
            BigDecimal usage30 = queryDecimal(
                "SELECT COALESCE(SUM(quantity),0) FROM inventory_transactions WHERE ingredient_id = ? AND transaction_type IN ('ISSUE','LOSS') AND transaction_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                ingredientId
            );
            BigDecimal avgDaily = usage30.divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
            BigDecimal lowStockThreshold = avgDaily.multiply(BigDecimal.valueOf(7));

            if (stock.compareTo(lowStockThreshold) < 0) {
                insertAlertIfMissing("LOW_STOCK", ingredientId, "Low stock below 7-day average usage");
                insertReplenishment(ingredientId, defaultUnitId, usage30, avgDaily, usage30.subtract(stock).max(BigDecimal.ZERO));
            }

            Integer nearExpiryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_batches WHERE ingredient_id = ? AND status = 'AVAILABLE' AND expires_at IS NOT NULL AND expires_at <= DATE_ADD(CURDATE(), INTERVAL 10 DAY)",
                Integer.class,
                ingredientId
            );
            if (nearExpiryCount != null && nearExpiryCount > 0) {
                insertAlertIfMissing("NEAR_EXPIRY", ingredientId, "Near-expiry batches within 10 days");
            }
        }
    }

    public List<Map<String, Object>> alertsView() {
        return jdbcTemplate.queryForList(
            "SELECT alert_type, severity, entity_id, message, created_at FROM system_alerts ORDER BY created_at DESC LIMIT 100"
        );
    }

    public List<Map<String, Object>> recommendationsView() {
        return jdbcTemplate.queryForList(
            "SELECT ingredient_id, trailing_30d_consumption, avg_daily_usage, suggested_reorder_qty, unit_id, generated_at FROM replenishment_recommendations ORDER BY generated_at DESC LIMIT 100"
        );
    }

    private Optional<BigDecimal> resolveSupplierPrice(Long supplierId, Long ingredientId) {
        return supplierItemRepository.findBySupplierIdAndIngredientId(supplierId, ingredientId)
            .flatMap(item -> supplierPriceHistoryRepository.findActivePriceHistory(item.getId(), LocalDate.now()).stream().findFirst())
            .map(SupplierPriceHistoryEntity::getPrice);
    }

    private void insertInventoryBatchAndTransaction(PurchaseOrderEntity po, PurchaseOrderLineEntity poLine, ReceiveLine line, BigDecimal acceptedQty) {
        Long ingredientDefaultUnitId = unitConversionService.ingredientDefaultUnitId(poLine.getIngredientId());
        BigDecimal normalizedQty = unitConversionService.convertQuantity(acceptedQty, poLine.getUnitId(), ingredientDefaultUnitId);
        BigDecimal normalizedUnitCost = unitConversionService.normalizeUnitCost(poLine.getUnitPrice(), poLine.getUnitId(), ingredientDefaultUnitId);

        Long supplierId = po.getSupplierId();
        jdbcTemplate.update(
            "INSERT INTO inventory_batches (ingredient_id, batch_no, supplier_id, quantity_received, quantity_available, unit_id, unit_cost, received_at, expires_at, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,'AVAILABLE',NOW(6),NOW(6))",
            poLine.getIngredientId(),
            line.batchNo(),
            supplierId,
            normalizedQty,
            normalizedQty,
            ingredientDefaultUnitId,
            normalizedUnitCost,
            LocalDateTime.now(),
            line.expiresAt() == null ? null : Date.valueOf(line.expiresAt())
        );

        Long batchId = jdbcTemplate.queryForObject(
            "SELECT id FROM inventory_batches WHERE ingredient_id = ? AND batch_no = ? ORDER BY id DESC LIMIT 1",
            Long.class,
            poLine.getIngredientId(),
            line.batchNo()
        );

        jdbcTemplate.update(
            "INSERT INTO inventory_transactions (ingredient_id, batch_id, transaction_type, quantity, unit_id, unit_cost, reference_type, reference_id, note, transaction_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,NOW(6),?)",
            poLine.getIngredientId(),
            batchId,
            "RECEIVE",
            normalizedQty,
            ingredientDefaultUnitId,
            normalizedUnitCost,
            "GOODS_RECEIPT",
            po.getId(),
            "Auto-posted from goods receipt",
            currentUserId()
        );
    }

    private void insertAlertIfMissing(String alertType, Long ingredientId, String message) {
        jdbcTemplate.update(
            "INSERT INTO system_alerts (alert_type, severity, entity_type, entity_id, message, created_at) "
                + "SELECT ?, 'HIGH', 'INGREDIENT', ?, ?, NOW(6) FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM system_alerts WHERE alert_type = ? AND entity_type = 'INGREDIENT' AND entity_id = ? AND acknowledged = FALSE AND created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY))",
            alertType,
            ingredientId,
            message,
            alertType,
            ingredientId
        );
    }

    private void insertReplenishment(Long ingredientId, Long unitId, BigDecimal trailing30, BigDecimal avgDaily, BigDecimal suggested) {
        jdbcTemplate.update(
            "INSERT INTO replenishment_recommendations (ingredient_id, generated_at, trailing_30d_consumption, avg_daily_usage, suggested_reorder_qty, unit_id, confidence_score, rationale) VALUES (?,?,?,?,?,?,?,JSON_OBJECT('method','trailing_30_day'))",
            ingredientId,
            LocalDateTime.now(),
            trailing30.setScale(3, RoundingMode.HALF_UP),
            avgDaily.setScale(3, RoundingMode.HALF_UP),
            suggested.setScale(3, RoundingMode.HALF_UP),
            unitId,
            BigDecimal.valueOf(82.5)
        );
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long currentUserId() {
        return userIdentityService.resolveCurrentUserId()
            .orElseThrow(() -> new IllegalStateException("Authenticated user context is required but not available"));
    }

    public record ReceiveRequest(Long purchaseOrderId, String note, List<ReceiveLine> lines) {
    }

    public record ReceiveLine(Long purchaseOrderLineId, String batchNo, BigDecimal acceptedQty, BigDecimal rejectedQty, LocalDate expiresAt) {
    }
}
