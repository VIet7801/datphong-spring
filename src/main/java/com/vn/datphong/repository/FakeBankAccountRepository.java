package com.vn.datphong.repository;

import com.vn.datphong.entity.FakeBankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface FakeBankAccountRepository extends JpaRepository<FakeBankAccount, Long> {
    Optional<FakeBankAccount> findByAccountNo(String accountNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select a from FakeBankAccount a where a.accountNo = :accountNo")
    Optional<FakeBankAccount> findForUpdate(@Param("accountNo") String accountNo);
}