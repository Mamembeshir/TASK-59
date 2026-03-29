CREATE INDEX idx_proc_requests_status_requested_at ON procurement_requests (status, requested_at);
CREATE INDEX idx_proc_request_lines_request_id ON procurement_request_lines (request_id);
CREATE INDEX idx_purchase_orders_request_status ON purchase_orders (request_id, status);
CREATE INDEX idx_purchase_order_lines_po_id ON purchase_order_lines (purchase_order_id);
CREATE INDEX idx_goods_receipts_po_received_at ON goods_receipts (purchase_order_id, received_at);
CREATE INDEX idx_goods_receipt_lines_receipt_id ON goods_receipt_lines (goods_receipt_id);

CREATE INDEX idx_system_alerts_entity_ack_window ON system_alerts (alert_type, entity_type, entity_id, acknowledged, created_at);
CREATE INDEX idx_inventory_batches_expiry_window ON inventory_batches (ingredient_id, status, expires_at);
CREATE INDEX idx_inventory_txn_consumption_window ON inventory_transactions (ingredient_id, transaction_type, transaction_at);
CREATE INDEX idx_replenishment_generated_at ON replenishment_recommendations (generated_at);

INSERT INTO suppliers (supplier_code, supplier_name, contact_name, active)
SELECT 'SUP-TOMATO', 'Tomato Fresh Supply Co', 'Marta Lane', TRUE
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE supplier_code = 'SUP-TOMATO');

INSERT INTO supplier_items (supplier_id, ingredient_id, supplier_sku, pack_size, pack_unit_id, preferred, active)
SELECT s.id, i.id, 'TOMATO-CASE-24', 24.000, u.id, TRUE, TRUE
FROM suppliers s
JOIN ingredients i ON i.ingredient_code = 'TOMATO'
JOIN inventory_units u ON u.unit_code = 'EA'
WHERE s.supplier_code = 'SUP-TOMATO'
  AND NOT EXISTS (
    SELECT 1
    FROM supplier_items si
    WHERE si.supplier_id = s.id
      AND si.ingredient_id = i.id
      AND si.pack_unit_id = u.id
  );

INSERT INTO supplier_price_history (supplier_item_id, price, currency_code, effective_from)
SELECT si.id, 0.3300, 'USD', CURDATE()
FROM supplier_items si
JOIN suppliers s ON s.id = si.supplier_id
JOIN ingredients i ON i.id = si.ingredient_id
WHERE s.supplier_code = 'SUP-TOMATO'
  AND i.ingredient_code = 'TOMATO'
  AND NOT EXISTS (
    SELECT 1
    FROM supplier_price_history sph
    WHERE sph.supplier_item_id = si.id
      AND sph.effective_from = CURDATE()
  );
