package com.instituteops.store.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.instituteops.recommender.domain.RecommenderService;
import com.instituteops.security.UserIdentityService;
import com.instituteops.security.repo.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SpuCatalogRepository spuCatalogRepository;
    @Mock
    private SkuCatalogRepository skuCatalogRepository;
    @Mock
    private SkuPricingTierRepository skuPricingTierRepository;
    @Mock
    private GroupBuyCampaignRepository groupBuyCampaignRepository;
    @Mock
    private GroupBuyGroupRepository groupBuyGroupRepository;
    @Mock
    private GroupBuyGroupMemberRepository groupBuyGroupMemberRepository;
    @Mock
    private GroupBuyOrderRepository groupBuyOrderRepository;
    @Mock
    private InventoryLockRepository inventoryLockRepository;
    @Mock
    private UserIdentityService userIdentityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private RecommenderService recommenderService;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(
            spuCatalogRepository,
            skuCatalogRepository,
            skuPricingTierRepository,
            groupBuyCampaignRepository,
            groupBuyGroupRepository,
            groupBuyGroupMemberRepository,
            groupBuyOrderRepository,
            inventoryLockRepository,
            userIdentityService,
            userRepository,
            jdbcTemplate,
            recommenderService
        );
    }

    @Test
    void placeOrder_enforcesPerStudentCampaignLimit() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("student1", "n/a"));

        GroupBuyCampaignEntity campaign = new GroupBuyCampaignEntity();
        campaign.setStatus("ACTIVE");
        campaign.setStartsAt(LocalDateTime.now().minusHours(1));
        campaign.setEndsAt(LocalDateTime.now().plusHours(8));
        campaign.setCutoffTime(LocalTime.now().plusHours(1));
        campaign.setSkuId(8L);

        SkuCatalogEntity sku = new SkuCatalogEntity();
        sku.setPurchaseLimitPerStudent(2);

        when(groupBuyCampaignRepository.findAll()).thenReturn(List.of(campaign));
        when(groupBuyCampaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(skuCatalogRepository.findById(8L)).thenReturn(Optional.of(sku));
        when(groupBuyOrderRepository.sumCommittedQtyForStudent(1L, null)).thenReturn(2);
        when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class))).thenReturn(List.of(1L));

        assertThatThrownBy(() -> service.placeOrder(new CatalogService.PlaceOrderRequest(1L, null, 1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Purchase limit exceeded");
    }
}
