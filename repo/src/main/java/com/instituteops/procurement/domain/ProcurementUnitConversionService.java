package com.instituteops.procurement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ProcurementUnitConversionService {

    private final JdbcTemplate jdbcTemplate;

    ProcurementUnitConversionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Long ingredientDefaultUnitId(Long ingredientId) {
        Long unitId = jdbcTemplate.queryForObject(
            "SELECT default_unit_id FROM ingredients WHERE id = ?",
            Long.class,
            ingredientId
        );
        if (unitId == null) {
            throw new IllegalArgumentException("Ingredient not found for inventory posting");
        }
        return unitId;
    }

    BigDecimal normalizeUnitCost(BigDecimal unitCost, Long fromUnitId, Long toUnitId) {
        BigDecimal factor = conversionFactor(fromUnitId, toUnitId);
        return unitCost.divide(factor, 4, RoundingMode.HALF_UP);
    }

    BigDecimal convertQuantity(BigDecimal qty, Long fromUnitId, Long toUnitId) {
        if (qty == null || fromUnitId == null || toUnitId == null) {
            throw new IllegalArgumentException("Quantity and unit ids are required");
        }
        if (fromUnitId.equals(toUnitId)) {
            return qty.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal factor = conversionFactor(fromUnitId, toUnitId);
        return qty.multiply(factor).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal conversionFactor(Long fromUnitId, Long toUnitId) {
        if (fromUnitId.equals(toUnitId)) {
            return BigDecimal.ONE;
        }
        List<BigDecimal> direct = jdbcTemplate.query(
            "SELECT factor FROM unit_conversions WHERE from_unit_id = ? AND to_unit_id = ? ORDER BY id DESC LIMIT 1",
            (rs, rowNum) -> rs.getBigDecimal("factor"),
            fromUnitId,
            toUnitId
        );
        if (!direct.isEmpty()) {
            return direct.getFirst();
        }
        List<BigDecimal> reverse = jdbcTemplate.query(
            "SELECT factor FROM unit_conversions WHERE from_unit_id = ? AND to_unit_id = ? ORDER BY id DESC LIMIT 1",
            (rs, rowNum) -> rs.getBigDecimal("factor"),
            toUnitId,
            fromUnitId
        );
        if (!reverse.isEmpty()) {
            return BigDecimal.ONE.divide(reverse.getFirst(), 6, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("No conversion path between selected units");
    }
}
