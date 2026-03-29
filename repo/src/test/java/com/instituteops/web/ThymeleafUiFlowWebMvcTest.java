package com.instituteops.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.instituteops.audit.AuditLogService;
import com.instituteops.audit.RequestAuditFilter;
import com.instituteops.governance.domain.GovernanceService;
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
import com.instituteops.student.model.StudentModuleService;
import com.instituteops.student.web.StudentController;
import com.instituteops.web.GovernancePageController;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    HomeController.class,
    StudentController.class,
    GovernancePageController.class,
    GradeEntryController.class,
    InventoryController.class,
    ProcurementController.class,
    RecommenderController.class,
    CatalogController.class
})
@AutoConfigureMockMvc(addFilters = false)
class ThymeleafUiFlowWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentModuleService studentModuleService;
    @MockBean
    private GradeEntryService gradeEntryService;
    @MockBean
    private InventoryModuleService inventoryModuleService;
    @MockBean
    private ProcurementService procurementService;
    @MockBean
    private RecommenderService recommenderService;
    @MockBean
    private CatalogService catalogService;
    @MockBean
    private GovernanceService governanceService;
    @MockBean
    private JdbcTemplate jdbcTemplate;
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

    @BeforeEach
    void seedViewModelMocks() {
        org.mockito.Mockito.when(recommenderService.recommendationsForCurrentUser(org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        org.mockito.Mockito.when(recommenderService.adminView()).thenReturn(new RecommenderService.AdminView(List.of()));
        org.mockito.Mockito.when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Number.class)))
            .thenReturn(0L);

        org.mockito.Mockito.when(studentModuleService.searchStudents(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        org.mockito.Mockito.when(gradeEntryService.latestLedger()).thenReturn(List.of());

        org.mockito.Mockito.when(inventoryModuleService.units()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.conversions()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.ingredients()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.batches()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.transactions()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.stockCounts()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.stockVarianceViews()).thenReturn(List.of());
        org.mockito.Mockito.when(inventoryModuleService.currentStockByIngredient()).thenReturn(Map.of());

        org.mockito.Mockito.when(procurementService.suppliers()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.supplierItems()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.priceHistory()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.requests()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.purchaseOrders()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.receipts()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.alertsView()).thenReturn(List.of());
        org.mockito.Mockito.when(procurementService.recommendationsView()).thenReturn(List.of());

        org.mockito.Mockito.when(catalogService.managerView()).thenReturn(new CatalogService.ManagerView(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));
        org.mockito.Mockito.when(catalogService.studentView()).thenReturn(new CatalogService.StudentView(List.of(), List.of()));
        org.mockito.Mockito.when(governanceService.latestDuplicateCandidates()).thenReturn(List.of());
        org.mockito.Mockito.when(governanceService.recycleBin()).thenReturn(List.of());
    }

    @Test
    void loginAndDashboardPagesRender() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("InstituteOps")));

        mockMvc.perform(get("/dashboard").with(authentication(new UsernamePasswordAuthenticationToken("sysadmin", "n/a", List.of()))))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Active Students")));
    }

    @Test
    void majorModulePagesRenderForRepresentativeFlows() throws Exception {
        mockMvc.perform(get("/student")).andExpect(status().isOk());
        mockMvc.perform(get("/instructor/grades")).andExpect(status().isOk());
        mockMvc.perform(get("/inventory")).andExpect(status().isOk());
        mockMvc.perform(get("/procurement")).andExpect(status().isOk());
        mockMvc.perform(get("/store")).andExpect(status().isOk());
        mockMvc.perform(get("/store/student")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/recommender")).andExpect(status().isOk());
        mockMvc.perform(get("/governance")).andExpect(status().isOk());
    }
}
