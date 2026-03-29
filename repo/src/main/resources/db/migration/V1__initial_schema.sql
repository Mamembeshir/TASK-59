CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARBINARY(512) NULL,
    phone VARBINARY(512) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    deleted_by BIGINT NULL,
    INDEX idx_users_deleted_at (deleted_at)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by BIGINT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS internal_api_clients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_key VARCHAR(128) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NULL
);

CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    preferred_name VARCHAR(100) NULL,
    date_of_birth DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    contact_email_encrypted VARBINARY(1024) NULL,
    contact_phone_encrypted VARBINARY(1024) NULL,
    contact_address_encrypted VARBINARY(2048) NULL,
    masked_email VARCHAR(255) NULL,
    masked_phone VARCHAR(64) NULL,
    emergency_contact_encrypted VARBINARY(2048) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'GRADUATED', 'SUSPENDED', 'WITHDRAWN')),
    INDEX idx_students_name_dob (last_name, first_name, date_of_birth),
    INDEX idx_students_deleted_at (deleted_at)
);

CREATE TABLE IF NOT EXISTS programs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    program_code VARCHAR(64) NOT NULL UNIQUE,
    program_name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    credits_required DECIMAL(8,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_code VARCHAR(64) NOT NULL UNIQUE,
    class_name VARCHAR(200) NOT NULL,
    program_id BIGINT NOT NULL,
    instructor_user_id BIGINT NULL,
    starts_on DATE NULL,
    ends_on DATE NULL,
    total_sessions INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_classes_program FOREIGN KEY (program_id) REFERENCES programs (id),
    CONSTRAINT fk_classes_instructor FOREIGN KEY (instructor_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS class_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    session_no INT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    topic VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_class_sessions (class_id, session_no),
    CONSTRAINT fk_class_sessions_class FOREIGN KEY (class_id) REFERENCES classes (id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    enrollment_status VARCHAR(32) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    withdrawn_at DATETIME(6) NULL,
    completion_date DATE NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT chk_enrollment_status CHECK (enrollment_status IN ('ENROLLED', 'WAITLISTED', 'WITHDRAWN', 'COMPLETED', 'FAILED')),
    UNIQUE KEY uq_enrollment_active (student_id, class_id, deleted_at),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES classes (id)
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    class_session_id BIGINT NOT NULL,
    attendance_status VARCHAR(32) NOT NULL,
    note VARCHAR(255) NULL,
    created_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_attendance_status CHECK (attendance_status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    UNIQUE KEY uq_attendance_once (enrollment_id, class_session_id),
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_attendance_session FOREIGN KEY (class_session_id) REFERENCES class_sessions (id)
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    enrollment_id BIGINT NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    payment_reference_encrypted VARBINARY(1024) NULL,
    recorded_by BIGINT NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    note VARCHAR(255) NULL,
    voided BOOLEAN NOT NULL DEFAULT FALSE,
    void_reason VARCHAR(255) NULL,
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'CHECK', 'OTHER_OFFLINE')),
    CONSTRAINT fk_payment_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_payment_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_payment_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id),
    INDEX idx_payment_student_time (student_id, recorded_at)
);

CREATE TABLE IF NOT EXISTS instructor_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    class_session_id BIGINT NULL,
    instructor_user_id BIGINT NOT NULL,
    comment_text TEXT NOT NULL,
    visibility VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_comment_visibility CHECK (visibility IN ('INTERNAL', 'STUDENT_VISIBLE')),
    CONSTRAINT fk_comments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_comments_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_comments_session FOREIGN KEY (class_session_id) REFERENCES class_sessions (id),
    CONSTRAINT fk_comments_instructor FOREIGN KEY (instructor_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS homework_attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    class_session_id BIGINT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sha256_checksum CHAR(64) NOT NULL,
    upload_path VARCHAR(1024) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_homework_mime CHECK (mime_type IN ('application/pdf', 'image/jpeg')),
    CONSTRAINT chk_homework_size CHECK (file_size_bytes <= 10485760),
    CONSTRAINT fk_homework_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_homework_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_homework_session FOREIGN KEY (class_session_id) REFERENCES class_sessions (id),
    CONSTRAINT fk_homework_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id),
    UNIQUE KEY uq_homework_checksum (sha256_checksum)
);

CREATE TABLE IF NOT EXISTS grade_rule_sets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ruleset_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from DATETIME(6) NOT NULL,
    effective_to DATETIME(6) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_grade_ruleset_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS grade_rule_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ruleset_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    rule_json JSON NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_grade_rule_version (ruleset_id, version_no),
    CONSTRAINT fk_grade_rule_versions_ruleset FOREIGN KEY (ruleset_id) REFERENCES grade_rule_sets (id),
    CONSTRAINT fk_grade_rule_versions_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS override_reason_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    immutable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS grade_ledger_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    class_session_id BIGINT NULL,
    enrollment_id BIGINT NULL,
    assessment_key VARCHAR(128) NOT NULL,
    raw_score DECIMAL(8,3) NOT NULL,
    max_score DECIMAL(8,3) NOT NULL,
    grade_letter VARCHAR(4) NULL,
    credits_earned DECIMAL(8,3) NOT NULL DEFAULT 0,
    gpa_points DECIMAL(8,3) NOT NULL DEFAULT 0,
    ruleset_id BIGINT NOT NULL,
    rule_version_id BIGINT NOT NULL,
    operation_type VARCHAR(32) NOT NULL DEFAULT 'ENTRY',
    operation_reason_code VARCHAR(64) NULL,
    previous_entry_id BIGINT NULL,
    delta_score DECIMAL(8,3) NULL,
    delta_credits DECIMAL(8,3) NULL,
    delta_gpa_points DECIMAL(8,3) NULL,
    entered_by BIGINT NOT NULL,
    entered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    append_only_marker CHAR(36) NOT NULL,
    CONSTRAINT chk_grade_operation_type CHECK (operation_type IN ('ENTRY', 'EDIT', 'RECALCULATION', 'MANUAL_OVERRIDE')),
    CONSTRAINT fk_grade_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_grade_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_grade_session FOREIGN KEY (class_session_id) REFERENCES class_sessions (id),
    CONSTRAINT fk_grade_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_grade_ruleset FOREIGN KEY (ruleset_id) REFERENCES grade_rule_sets (id),
    CONSTRAINT fk_grade_rule_version FOREIGN KEY (rule_version_id) REFERENCES grade_rule_versions (id),
    CONSTRAINT fk_grade_prev_entry FOREIGN KEY (previous_entry_id) REFERENCES grade_ledger_entries (id),
    CONSTRAINT fk_grade_entered_by FOREIGN KEY (entered_by) REFERENCES users (id),
    INDEX idx_grade_student_class_time (student_id, class_id, entered_at)
);

CREATE TABLE IF NOT EXISTS grade_recalculations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NULL,
    class_id BIGINT NULL,
    triggered_by BIGINT NOT NULL,
    triggered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    scope_json JSON NOT NULL,
    deterministic_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    CONSTRAINT chk_recalc_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'ROLLED_BACK')),
    CONSTRAINT fk_recalc_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_recalc_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_recalc_triggered_by FOREIGN KEY (triggered_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS grade_recalculation_deltas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recalculation_id BIGINT NOT NULL,
    grade_ledger_entry_id BIGINT NOT NULL,
    previous_result_json JSON NOT NULL,
    new_result_json JSON NOT NULL,
    delta_json JSON NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_recalc_deltas_recalc FOREIGN KEY (recalculation_id) REFERENCES grade_recalculations (id),
    CONSTRAINT fk_recalc_deltas_entry FOREIGN KEY (grade_ledger_entry_id) REFERENCES grade_ledger_entries (id)
);

CREATE TABLE IF NOT EXISTS inventory_units (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    unit_code VARCHAR(32) NOT NULL UNIQUE,
    unit_name VARCHAR(64) NOT NULL,
    unit_type VARCHAR(32) NOT NULL,
    base_unit BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_inventory_unit_type CHECK (unit_type IN ('COUNT', 'WEIGHT', 'VOLUME', 'PACKAGE'))
);

CREATE TABLE IF NOT EXISTS unit_conversions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_unit_id BIGINT NOT NULL,
    to_unit_id BIGINT NOT NULL,
    factor DECIMAL(18,6) NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_unit_conversion (from_unit_id, to_unit_id),
    CONSTRAINT fk_unit_conversions_from FOREIGN KEY (from_unit_id) REFERENCES inventory_units (id),
    CONSTRAINT fk_unit_conversions_to FOREIGN KEY (to_unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ingredient_code VARCHAR(64) NOT NULL UNIQUE,
    ingredient_name VARCHAR(200) NOT NULL,
    default_unit_id BIGINT NOT NULL,
    reorder_lead_days INT NOT NULL DEFAULT 7,
    low_stock_days_threshold INT NOT NULL DEFAULT 7,
    near_expiry_days_threshold INT NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ingredients_default_unit FOREIGN KEY (default_unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS inventory_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id BIGINT NOT NULL,
    batch_no VARCHAR(128) NOT NULL,
    supplier_id BIGINT NULL,
    quantity_received DECIMAL(14,3) NOT NULL,
    quantity_available DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NOT NULL,
    unit_cost DECIMAL(14,4) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    expires_at DATE NULL,
    shelf_life_days INT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_inventory_batch_status CHECK (status IN ('AVAILABLE', 'EXPIRED', 'DEPLETED', 'QUARANTINED')),
    UNIQUE KEY uq_inventory_batch (ingredient_id, batch_no),
    INDEX idx_inventory_batches_fifo (ingredient_id, expires_at, received_at),
    CONSTRAINT fk_inventory_batches_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_inventory_batches_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS loss_reason_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reason_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id BIGINT NOT NULL,
    batch_id BIGINT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NOT NULL,
    unit_cost DECIMAL(14,4) NULL,
    reference_type VARCHAR(64) NULL,
    reference_id BIGINT NULL,
    loss_reason_id BIGINT NULL,
    note VARCHAR(255) NULL,
    transaction_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NOT NULL,
    CONSTRAINT chk_inventory_txn_type CHECK (transaction_type IN ('RECEIVE', 'ISSUE', 'ADJUSTMENT', 'LOSS', 'RETURN', 'LOCK', 'UNLOCK')),
    CONSTRAINT fk_inventory_txn_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_inventory_txn_batch FOREIGN KEY (batch_id) REFERENCES inventory_batches (id),
    CONSTRAINT fk_inventory_txn_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id),
    CONSTRAINT fk_inventory_txn_loss_reason FOREIGN KEY (loss_reason_id) REFERENCES loss_reason_codes (id),
    CONSTRAINT fk_inventory_txn_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_inventory_txn_ingredient_time (ingredient_id, transaction_at)
);

CREATE TABLE IF NOT EXISTS stock_counts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    count_no VARCHAR(64) NOT NULL UNIQUE,
    counted_at DATETIME(6) NOT NULL,
    counted_by BIGINT NOT NULL,
    approved_by BIGINT NULL,
    variance_percent_threshold DECIMAL(8,3) NOT NULL DEFAULT 2.000,
    variance_value_threshold DECIMAL(12,2) NOT NULL DEFAULT 50.00,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_stock_count_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'POSTED')),
    CONSTRAINT fk_stock_counts_counted_by FOREIGN KEY (counted_by) REFERENCES users (id),
    CONSTRAINT fk_stock_counts_approved_by FOREIGN KEY (approved_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS stock_count_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_count_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    expected_qty DECIMAL(14,3) NOT NULL,
    actual_qty DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NOT NULL,
    expected_value DECIMAL(14,2) NOT NULL,
    actual_value DECIMAL(14,2) NOT NULL,
    variance_qty DECIMAL(14,3) NOT NULL,
    variance_value DECIMAL(14,2) NOT NULL,
    variance_percent DECIMAL(8,3) NOT NULL,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    line_note VARCHAR(255) NULL,
    UNIQUE KEY uq_stock_count_line (stock_count_id, ingredient_id),
    CONSTRAINT fk_stock_count_lines_count FOREIGN KEY (stock_count_id) REFERENCES stock_counts (id),
    CONSTRAINT fk_stock_count_lines_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_stock_count_lines_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_code VARCHAR(64) NOT NULL UNIQUE,
    supplier_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200) NULL,
    contact_phone_encrypted VARBINARY(1024) NULL,
    contact_email_encrypted VARBINARY(1024) NULL,
    address_encrypted VARBINARY(2048) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

ALTER TABLE inventory_batches
    ADD CONSTRAINT fk_inventory_batches_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id);

CREATE TABLE IF NOT EXISTS supplier_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    supplier_sku VARCHAR(100) NULL,
    pack_size DECIMAL(14,3) NOT NULL,
    pack_unit_id BIGINT NOT NULL,
    preferred BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_supplier_ingredient (supplier_id, ingredient_id, pack_unit_id),
    CONSTRAINT fk_supplier_items_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_supplier_items_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_supplier_items_pack_unit FOREIGN KEY (pack_unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS supplier_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_item_id BIGINT NOT NULL,
    price DECIMAL(14,4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_supplier_price_history_item FOREIGN KEY (supplier_item_id) REFERENCES supplier_items (id),
    INDEX idx_supplier_price_item_dates (supplier_item_id, effective_from, effective_to)
);

CREATE TABLE IF NOT EXISTS procurement_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL UNIQUE,
    requested_by BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    justification TEXT NULL,
    needed_by DATE NULL,
    CONSTRAINT chk_procurement_request_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'ORDERED', 'RECEIVED', 'CLOSED')),
    CONSTRAINT fk_procurement_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS procurement_request_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    requested_qty DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NOT NULL,
    estimated_unit_price DECIMAL(14,4) NULL,
    note VARCHAR(255) NULL,
    CONSTRAINT fk_procurement_request_lines_request FOREIGN KEY (request_id) REFERENCES procurement_requests (id),
    CONSTRAINT fk_procurement_request_lines_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_procurement_request_lines_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS procurement_approvals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    decision VARCHAR(32) NOT NULL,
    decision_note VARCHAR(255) NULL,
    decided_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_procurement_approval_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT fk_procurement_approvals_request FOREIGN KEY (request_id) REFERENCES procurement_requests (id),
    CONSTRAINT fk_procurement_approvals_user FOREIGN KEY (approver_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    po_no VARCHAR(64) NOT NULL UNIQUE,
    request_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    ordered_by BIGINT NOT NULL,
    ordered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    expected_delivery DATE NULL,
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    CONSTRAINT chk_purchase_order_status CHECK (status IN ('OPEN', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED', 'CLOSED')),
    CONSTRAINT fk_purchase_orders_request FOREIGN KEY (request_id) REFERENCES procurement_requests (id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchase_orders_ordered_by FOREIGN KEY (ordered_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS purchase_order_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    ordered_qty DECIMAL(14,3) NOT NULL,
    received_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
    unit_id BIGINT NOT NULL,
    unit_price DECIMAL(14,4) NOT NULL,
    line_amount DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_purchase_order_lines_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_purchase_order_lines_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_purchase_order_lines_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_no VARCHAR(64) NOT NULL UNIQUE,
    purchase_order_id BIGINT NOT NULL,
    received_by BIGINT NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(32) NOT NULL DEFAULT 'POSTED',
    note VARCHAR(255) NULL,
    CONSTRAINT chk_goods_receipts_status CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED')),
    CONSTRAINT fk_goods_receipts_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_goods_receipts_received_by FOREIGN KEY (received_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS goods_receipt_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_receipt_id BIGINT NOT NULL,
    purchase_order_line_id BIGINT NOT NULL,
    batch_no VARCHAR(128) NOT NULL,
    received_qty DECIMAL(14,3) NOT NULL,
    accepted_qty DECIMAL(14,3) NOT NULL,
    rejected_qty DECIMAL(14,3) NOT NULL DEFAULT 0,
    unit_id BIGINT NOT NULL,
    unit_cost DECIMAL(14,4) NOT NULL,
    expires_at DATE NULL,
    CONSTRAINT fk_goods_receipt_lines_receipt FOREIGN KEY (goods_receipt_id) REFERENCES goods_receipts (id),
    CONSTRAINT fk_goods_receipt_lines_po_line FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_lines (id),
    CONSTRAINT fk_goods_receipt_lines_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS spu_catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    spu_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS sku_catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    spu_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    specs JSON NULL,
    inventory_item_ref VARCHAR(128) NULL,
    purchase_limit_per_student INT NOT NULL DEFAULT 5,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sku_catalog_spu FOREIGN KEY (spu_id) REFERENCES spu_catalog (id)
);

CREATE TABLE IF NOT EXISTS sku_pricing_tiers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_id BIGINT NOT NULL,
    min_qty INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_sku_tier_min_qty (sku_id, min_qty, valid_from),
    CONSTRAINT fk_sku_pricing_tiers_sku FOREIGN KEY (sku_id) REFERENCES sku_catalog (id)
);

CREATE TABLE IF NOT EXISTS group_buy_campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_code VARCHAR(64) NOT NULL UNIQUE,
    sku_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    cutoff_time TIME NOT NULL DEFAULT '21:00:00',
    required_participants INT NOT NULL DEFAULT 10,
    formation_window_hours INT NOT NULL DEFAULT 72,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_campaign_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUCCEEDED', 'FAILED', 'CLOSED')),
    CONSTRAINT fk_group_buy_campaigns_sku FOREIGN KEY (sku_id) REFERENCES sku_catalog (id),
    CONSTRAINT fk_group_buy_campaigns_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS group_buy_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    initiator_student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'FORMING',
    participants_target INT NOT NULL,
    participants_current INT NOT NULL DEFAULT 1,
    formed_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_group_status CHECK (status IN ('FORMING', 'FORMED', 'EXPIRED', 'FAILED', 'CLOSED')),
    CONSTRAINT fk_group_buy_groups_campaign FOREIGN KEY (campaign_id) REFERENCES group_buy_campaigns (id),
    CONSTRAINT fk_group_buy_groups_initiator FOREIGN KEY (initiator_student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS group_buy_group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(32) NOT NULL DEFAULT 'JOINED',
    UNIQUE KEY uq_group_buy_member (group_id, student_id),
    CONSTRAINT chk_group_member_status CHECK (status IN ('JOINED', 'LEFT', 'VOID')),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES group_buy_groups (id),
    CONSTRAINT fk_group_members_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS group_buy_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    campaign_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_GROUP',
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_captured BOOLEAN NOT NULL DEFAULT FALSE,
    void_reason VARCHAR(255) NULL,
    placed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_group_buy_order_status CHECK (order_status IN ('PENDING_GROUP', 'INVENTORY_LOCKED', 'CONFIRMED', 'VOID', 'CANCELLED')),
    CONSTRAINT fk_group_buy_orders_campaign FOREIGN KEY (campaign_id) REFERENCES group_buy_campaigns (id),
    CONSTRAINT fk_group_buy_orders_group FOREIGN KEY (group_id) REFERENCES group_buy_groups (id),
    CONSTRAINT fk_group_buy_orders_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE IF NOT EXISTS inventory_locks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_buy_order_id BIGINT NOT NULL,
    ingredient_id BIGINT NULL,
    sku_id BIGINT NULL,
    locked_qty DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NULL,
    lock_status VARCHAR(32) NOT NULL DEFAULT 'LOCKED',
    lock_reason VARCHAR(255) NOT NULL,
    locked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    released_at DATETIME(6) NULL,
    CONSTRAINT chk_inventory_locks_status CHECK (lock_status IN ('LOCKED', 'RELEASED', 'CONSUMED')),
    CONSTRAINT fk_inventory_locks_order FOREIGN KEY (group_buy_order_id) REFERENCES group_buy_orders (id),
    CONSTRAINT fk_inventory_locks_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_inventory_locks_sku FOREIGN KEY (sku_id) REFERENCES sku_catalog (id),
    CONSTRAINT fk_inventory_locks_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS system_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    message VARCHAR(255) NOT NULL,
    details JSON NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT NULL,
    acknowledged_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_alert_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT fk_system_alerts_ack_by FOREIGN KEY (acknowledged_by) REFERENCES users (id),
    INDEX idx_system_alert_type_created (alert_type, created_at)
);

CREATE TABLE IF NOT EXISTS replenishment_recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id BIGINT NOT NULL,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    trailing_30d_consumption DECIMAL(14,3) NOT NULL,
    avg_daily_usage DECIMAL(14,3) NOT NULL,
    suggested_reorder_qty DECIMAL(14,3) NOT NULL,
    unit_id BIGINT NOT NULL,
    confidence_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    rationale JSON NULL,
    CONSTRAINT fk_replenishment_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT fk_replenishment_unit FOREIGN KEY (unit_id) REFERENCES inventory_units (id)
);

CREATE TABLE IF NOT EXISTS bulk_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_type VARCHAR(32) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_checksum CHAR(64) NOT NULL,
    started_by BIGINT NOT NULL,
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    summary JSON NULL,
    CONSTRAINT chk_bulk_job_type CHECK (job_type IN ('IMPORT', 'EXPORT')),
    CONSTRAINT chk_bulk_job_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL')),
    CONSTRAINT fk_bulk_jobs_started_by FOREIGN KEY (started_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS consistency_issues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    issue_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    issue_details JSON NOT NULL,
    detected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by BIGINT NULL,
    resolved_at DATETIME(6) NULL,
    CONSTRAINT fk_consistency_issues_resolved_by FOREIGN KEY (resolved_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS duplicate_detection_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_entity_type VARCHAR(64) NOT NULL,
    source_entity_id BIGINT NOT NULL,
    matched_entity_type VARCHAR(64) NOT NULL,
    matched_entity_id BIGINT NOT NULL,
    match_mode VARCHAR(32) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    matched_on VARCHAR(255) NOT NULL,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_duplicate_match_mode CHECK (match_mode IN ('EXACT', 'FUZZY')),
    CONSTRAINT fk_duplicate_results_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    INDEX idx_duplicate_source (source_entity_type, source_entity_id)
);

CREATE TABLE IF NOT EXISTS change_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    old_data JSON NULL,
    new_data JSON NULL,
    reason_code VARCHAR(64) NULL,
    CONSTRAINT chk_change_history_operation CHECK (operation IN ('CREATE', 'UPDATE', 'SOFT_DELETE', 'RESTORE')),
    CONSTRAINT fk_change_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id),
    INDEX idx_change_history_entity (entity_type, entity_id, changed_at)
);

CREATE TABLE IF NOT EXISTS recycle_bin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    deleted_by BIGINT NOT NULL,
    deleted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    purge_after DATETIME(6) NOT NULL,
    payload JSON NOT NULL,
    restored BOOLEAN NOT NULL DEFAULT FALSE,
    restored_by BIGINT NULL,
    restored_at DATETIME(6) NULL,
    CONSTRAINT fk_recycle_bin_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id),
    CONSTRAINT fk_recycle_bin_restored_by FOREIGN KEY (restored_by) REFERENCES users (id),
    INDEX idx_recycle_bin_purge_after (purge_after)
);

CREATE TABLE IF NOT EXISTS sync_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_name VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    lan_only BOOLEAN NOT NULL DEFAULT TRUE,
    cron_expression VARCHAR(64) NULL,
    endpoint VARCHAR(255) NULL,
    api_key_hash VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    actor_username VARCHAR(64) NULL,
    role_snapshot VARCHAR(255) NULL,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64) NULL,
    entity_id BIGINT NULL,
    request_id VARCHAR(64) NULL,
    client_ip VARCHAR(64) NULL,
    success BOOLEAN NOT NULL,
    message VARCHAR(255) NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_operation_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    INDEX idx_operation_logs_action_time (action, occurred_at)
);

CREATE TABLE IF NOT EXISTS data_access_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    access_type VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    request_id VARCHAR(64) NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_data_access_type CHECK (access_type IN ('READ', 'EXPORT', 'MASKED_READ', 'UNMASKED_READ')),
    CONSTRAINT fk_data_access_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id),
    INDEX idx_data_access_entity_time (entity_type, entity_id, occurred_at)
);

CREATE TABLE IF NOT EXISTS recommender_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(32) NOT NULL,
    student_id BIGINT NULL,
    item_type VARCHAR(64) NOT NULL,
    item_id BIGINT NOT NULL,
    event_value DECIMAL(12,4) NOT NULL DEFAULT 1,
    occurred_at DATETIME(6) NOT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'LOCAL',
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at DATETIME(6) NULL,
    CONSTRAINT chk_recommender_event_type CHECK (event_type IN ('VIEW', 'ORDER', 'ENROLLMENT')),
    CONSTRAINT fk_recommender_events_student FOREIGN KEY (student_id) REFERENCES students (id),
    INDEX idx_recommender_events_processed (processed, occurred_at),
    INDEX idx_recommender_events_student_item (student_id, item_type, item_id)
);

CREATE TABLE IF NOT EXISTS recommender_models (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_code VARCHAR(64) NOT NULL UNIQUE,
    algorithm_family VARCHAR(16) NOT NULL,
    similarity_metric VARCHAR(32) NOT NULL,
    time_decay_half_life_days INT NOT NULL DEFAULT 14,
    popularity_penalty DECIMAL(8,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_recommender_algorithm_family CHECK (algorithm_family IN ('USER_CF', 'ITEM_CF')),
    CONSTRAINT chk_recommender_similarity_metric CHECK (similarity_metric IN ('COSINE', 'JACCARD', 'ADJUSTED_COSINE')),
    CONSTRAINT fk_recommender_models_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS recommender_model_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    trained_from DATETIME(6) NOT NULL,
    trained_to DATETIME(6) NOT NULL,
    config_json JSON NOT NULL,
    quality_metrics_json JSON NULL,
    training_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    rollback_of_version BIGINT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_recommender_model_version (model_id, version_no),
    CONSTRAINT chk_recommender_model_version_status CHECK (training_status IN ('RUNNING', 'COMPLETED', 'FAILED', 'ROLLED_BACK')),
    CONSTRAINT fk_recommender_model_versions_model FOREIGN KEY (model_id) REFERENCES recommender_models (id),
    CONSTRAINT fk_recommender_model_versions_rollback FOREIGN KEY (rollback_of_version) REFERENCES recommender_model_versions (id),
    CONSTRAINT fk_recommender_model_versions_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS recommender_recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_version_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    item_id BIGINT NOT NULL,
    score DECIMAL(12,6) NOT NULL,
    rank_no INT NOT NULL,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_recommender_recommendations_model_version FOREIGN KEY (model_version_id) REFERENCES recommender_model_versions (id),
    CONSTRAINT fk_recommender_recommendations_student FOREIGN KEY (student_id) REFERENCES students (id),
    UNIQUE KEY uq_recommendation_rank (model_version_id, student_id, rank_no),
    INDEX idx_recommendation_student (student_id, generated_at)
);

CREATE TABLE IF NOT EXISTS recommender_incremental_updates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_version_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    applied_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    details JSON NULL,
    CONSTRAINT chk_recommender_incremental_status CHECK (status IN ('APPLIED', 'SKIPPED', 'FAILED')),
    CONSTRAINT fk_recommender_incremental_model_version FOREIGN KEY (model_version_id) REFERENCES recommender_model_versions (id),
    CONSTRAINT fk_recommender_incremental_event FOREIGN KEY (event_id) REFERENCES recommender_events (id),
    UNIQUE KEY uq_incremental_event_model (model_version_id, event_id)
);

INSERT INTO roles (role_code, role_name) VALUES
('SYSTEM_ADMIN', 'System Administrator'),
('REGISTRAR_FINANCE_CLERK', 'Registrar/Finance Clerk'),
('INSTRUCTOR', 'Instructor'),
('INVENTORY_MANAGER', 'Inventory Manager'),
('PROCUREMENT_APPROVER', 'Procurement Approver'),
('STORE_MANAGER', 'Store Manager'),
('STUDENT', 'Student');

INSERT INTO loss_reason_codes (reason_code, description) VALUES
('SPOILAGE', 'Spoilage'),
('DAMAGE', 'Damage'),
('THEFT', 'Theft'),
('PREP_WASTE', 'Preparation Waste');

INSERT INTO override_reason_codes (reason_code, description, immutable) VALUES
('POLICY_EXCEPTION', 'Policy exception approved by authorized reviewer', TRUE),
('DATA_CORRECTION', 'Source data correction after documented review', TRUE),
('APPEAL_DECISION', 'Grade changed due to formal appeal outcome', TRUE);

INSERT INTO inventory_units (unit_code, unit_name, unit_type, base_unit) VALUES
('EA', 'Each', 'COUNT', TRUE),
('CASE', 'Case', 'PACKAGE', FALSE),
('LB', 'Pound', 'WEIGHT', FALSE),
('OZ', 'Ounce', 'WEIGHT', TRUE);
