package com.instituteops.recommender.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecommenderEventRepository extends JpaRepository<RecommenderEventEntity, Long> {
}

interface RecommenderModelRepository extends JpaRepository<RecommenderModelEntity, Long> {
}

interface RecommenderModelVersionRepository extends JpaRepository<RecommenderModelVersionEntity, Long> {
}

interface RecommenderRecommendationRepository extends JpaRepository<RecommenderRecommendationEntity, Long> {
}

interface RecommenderIncrementalUpdateRepository extends JpaRepository<RecommenderIncrementalUpdateEntity, Long> {
}
