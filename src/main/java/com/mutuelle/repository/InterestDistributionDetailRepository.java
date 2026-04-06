package com.mutuelle.repository;

import com.mutuelle.entity.InterestDistributionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestDistributionDetailRepository extends JpaRepository<InterestDistributionDetail, Long> {
    List<InterestDistributionDetail> findByDistributionId(Long distributionId);
    List<InterestDistributionDetail> findByMemberId(Long memberId);
}
