package com.vn.datphong.repository;

import com.vn.datphong.entity.FakeBankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
@Repository
public interface FakeBankTransactionRepository extends JpaRepository<FakeBankTransaction, Long> {
    Optional<FakeBankTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<FakeBankTransaction> findByReference(String reference);

    @Query("select coalesce(sum(t.amount), 0) from FakeBankTransaction t where t.type = 'REFUND' and t.status = 'SUCCESS' and t.relatedReference = :ref")
    BigDecimal sumRefunded(@Param("ref") String ref);
}
