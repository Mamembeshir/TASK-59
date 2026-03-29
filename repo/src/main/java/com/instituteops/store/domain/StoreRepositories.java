package com.instituteops.store.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpuCatalogRepository extends JpaRepository<SpuCatalogEntity, Long> {
}

interface SkuCatalogRepository extends JpaRepository<SkuCatalogEntity, Long> {
}

interface SkuPricingTierRepository extends JpaRepository<SkuPricingTierEntity, Long> {
}

interface GroupBuyCampaignRepository extends JpaRepository<GroupBuyCampaignEntity, Long> {
}

interface GroupBuyGroupRepository extends JpaRepository<GroupBuyGroupEntity, Long> {
}

interface GroupBuyGroupMemberRepository extends JpaRepository<GroupBuyGroupMemberEntity, Long> {
}

interface GroupBuyOrderRepository extends JpaRepository<GroupBuyOrderEntity, Long> {
}

interface InventoryLockRepository extends JpaRepository<InventoryLockEntity, Long> {
}
