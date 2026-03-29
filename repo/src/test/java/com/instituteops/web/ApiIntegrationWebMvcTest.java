package com.instituteops.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituteops.audit.AuditLogService;
import com.instituteops.audit.RequestAuditFilter;
import com.instituteops.grades.domain.GradeEntryController;
import com.instituteops.grades.domain.GradeEntryService;
import com.instituteops.inventory.domain.InventoryController;
import com.instituteops.inventory.domain.InventoryModuleService;
import com.instituteops.procurement.domain.ProcurementController;
import com.instituteops.procurement.domain.ProcurementService;
import com.instituteops.recommender.domain.RecommenderController;
import com.instituteops.recommender.domain.RecommenderService;
import com.instituteops.security.InstituteUserDetailsService;
import com.instituteops.security.InternalApiClientAuthFilter;
import com.instituteops.security.LoginSuccessHandler;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.InternalApiClientRepository;
import com.instituteops.store.domain.CatalogController;
import com.instituteops.store.domain.CatalogService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    GradeEntryController.class,
    InventoryController.class,
    ProcurementController.class,
    CatalogController.class,
    RecommenderController.class
})
@AutoConfigureMockMvc(addFilters = false)
class ApiIntegrationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GradeEntryService gradeEntryService;
    @MockBean
    private InventoryModuleService inventoryModuleService;
    @MockBean
    private ProcurementService procurementService;
    @MockBean
    private CatalogService catalogService;
    @MockBean
    private RecommenderService recommenderService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private UserIdentityService userIdentityService;
    @MockBean
    private InternalApiClientRepository internalApiClientRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private InstituteUserDetailsService instituteUserDetailsService;
    @MockBean
    private LoginSuccessHandler loginSuccessHandler;
    @MockBean
    private InternalApiClientAuthFilter internalApiClientAuthFilter;
    @MockBean
    private RequestAuditFilter requestAuditFilter;

    @Test
    void gradesPreviewApi_returnsImpactPayload() throws Exception {
        org.mockito.Mockito.when(gradeEntryService.preview(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new GradeEntryService.GradeImpact(
                new BigDecimal("91.000"), "A", new BigDecimal("3.000"), new BigDecimal("12.000"), null, null, null
            ));

        mockMvc.perform(post("/api/grades/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"studentId":1,"classId":1,"assessmentKey":"MIDTERM","rawScore":91,"maxScore":100}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gradeLetter").value("A"));
    }

    @Test
    void inventoryAndProcurementApis_areReachable() throws Exception {
        org.mockito.Mockito.when(inventoryModuleService.currentStockByIngredient()).thenReturn(java.util.Map.of(1L, new BigDecimal("11.000")));
        org.mockito.Mockito.when(procurementService.recommendationsView()).thenReturn(List.of(java.util.Map.of("ingredient_id", 1, "suggested_reorder_qty", 9)));

        mockMvc.perform(get("/api/inventory/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.1").value(11.0));

        mockMvc.perform(get("/api/procurement/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ingredient_id").value(1));
    }

    @Test
    void storeAndRecommenderApis_returnExpectedContracts() throws Exception {
        org.mockito.Mockito.when(catalogService.quote(1L, 3))
            .thenReturn(new CatalogService.PriceQuote("MAT-KIT-001-A", 3, new BigDecimal("25.00"), new BigDecimal("75.00"), "USD", 5));
        org.mockito.Mockito.when(recommenderService.recommendationsForCurrentUser(5))
            .thenReturn(List.of(new RecommenderService.RecommendationView("SKU", 2L, 1, new BigDecimal("0.92"), "MAT-KIT-001-B")));

        mockMvc.perform(get("/api/store/quote").param("skuId", "1").param("quantity", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(75.0));

        mockMvc.perform(get("/api/recommender/me").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemLabel").value("MAT-KIT-001-B"));

        RecommenderController.RecordEventRequest event = new RecommenderController.RecordEventRequest(
            "ORDER", 1L, "SKU", 2L, new BigDecimal("1"), LocalDateTime.now(), "TEST"
        );
        mockMvc.perform(post("/api/recommender/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(event)))
            .andExpect(status().isOk());
    }
}
