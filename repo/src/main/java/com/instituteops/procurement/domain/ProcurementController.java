package com.instituteops.procurement.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @GetMapping("/procurement")
    public String dashboard(Model model) {
        procurementService.refreshAlertsAndRecommendations();
        model.addAttribute("suppliers", procurementService.suppliers());
        model.addAttribute("supplierItems", procurementService.supplierItems());
        model.addAttribute("priceHistory", procurementService.priceHistory());
        model.addAttribute("requests", procurementService.requests());
        model.addAttribute("purchaseOrders", procurementService.purchaseOrders());
        model.addAttribute("receipts", procurementService.receipts());
        model.addAttribute("alerts", procurementService.alertsView());
        model.addAttribute("recommendations", procurementService.recommendationsView());
        return "procurement-dashboard";
    }

    @PostMapping("/procurement/supplier")
    public String supplier(@RequestParam String supplierCode, @RequestParam String supplierName, @RequestParam(required = false) String contactName) {
        procurementService.createSupplier(supplierCode, supplierName, contactName);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/supplier-item")
    public String supplierItem(
        @RequestParam Long supplierId,
        @RequestParam Long ingredientId,
        @RequestParam(required = false) String supplierSku,
        @RequestParam BigDecimal packSize,
        @RequestParam Long packUnitId,
        @RequestParam(defaultValue = "false") boolean preferred
    ) {
        procurementService.createSupplierItem(supplierId, ingredientId, supplierSku, packSize, packUnitId, preferred);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/price")
    public String price(
        @RequestParam Long supplierItemId,
        @RequestParam BigDecimal price,
        @RequestParam(defaultValue = "USD") String currencyCode,
        @RequestParam LocalDate effectiveFrom,
        @RequestParam(required = false) LocalDate effectiveTo
    ) {
        procurementService.addPriceHistory(supplierItemId, price, currencyCode, effectiveFrom, effectiveTo);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/request")
    public String request(@RequestParam(required = false) String justification, @RequestParam(required = false) LocalDate neededBy) {
        procurementService.createRequest(justification, neededBy);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/request-line")
    public String requestLine(
        @RequestParam Long requestId,
        @RequestParam Long ingredientId,
        @RequestParam BigDecimal requestedQty,
        @RequestParam Long unitId,
        @RequestParam(required = false) BigDecimal estimatedUnitPrice,
        @RequestParam(required = false) String note
    ) {
        procurementService.addRequestLine(requestId, ingredientId, requestedQty, unitId, estimatedUnitPrice, note);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/approve")
    public String approve(@RequestParam Long requestId, @RequestParam String decision, @RequestParam(required = false) String decisionNote) {
        procurementService.approve(requestId, decision, decisionNote);
        return "redirect:/procurement";
    }

    @PostMapping("/procurement/order")
    public String order(@RequestParam Long requestId, @RequestParam Long supplierId) {
        procurementService.createPurchaseOrder(requestId, supplierId);
        return "redirect:/procurement";
    }

    @ResponseBody
    @PostMapping("/api/procurement/receive")
    public ResponseEntity<?> receiveApi(@RequestBody ProcurementService.ReceiveRequest request) {
        return ResponseEntity.ok(procurementService.receive(request));
    }

    @PostMapping("/procurement/receive")
    public String receive(
        @RequestParam Long purchaseOrderId,
        @RequestParam Long purchaseOrderLineId,
        @RequestParam String batchNo,
        @RequestParam BigDecimal acceptedQty,
        @RequestParam(required = false) BigDecimal rejectedQty,
        @RequestParam(required = false) LocalDate expiresAt,
        @RequestParam(required = false) String note
    ) {
        procurementService.receive(new ProcurementService.ReceiveRequest(
            purchaseOrderId,
            note,
            List.of(new ProcurementService.ReceiveLine(purchaseOrderLineId, batchNo, acceptedQty, rejectedQty, expiresAt))
        ));
        return "redirect:/procurement";
    }

    @ResponseBody
    @GetMapping("/api/procurement/recommendations")
    public Object recommendations() {
        procurementService.refreshAlertsAndRecommendations();
        return procurementService.recommendationsView();
    }
}
