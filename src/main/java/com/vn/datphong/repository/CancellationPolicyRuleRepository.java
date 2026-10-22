package com.vn.datphong.repository;

import com.vn.datphong.entity.CancellationPolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CancellationPolicyRuleRepository  extends JpaRepository<CancellationPolicyRule, Long> {
    @Query("select r from CancellationPolicyRule r where r.policy.id = :policyId order by r.daysBeforeCheckin desc")
    List<CancellationPolicyRule> findByPolicy(@Param("policyId") Long policyId);
}
