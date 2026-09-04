package com.ricardosenna.bankingapi.repository;

import com.ricardosenna.bankingapi.entity.TransactionEntity;
import com.ricardosenna.bankingapi.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Page<TransactionEntity> findAllByAccountId(Long accountId, Pageable pageable);

    Page<TransactionEntity> findAllByAccountIdOrderByMomentDesc(Long accountId, Pageable pageable);

    @Query("select t from TransactionEntity t where t.account.id = :accountId " +
            "and (:type is null or t.type = :type) " +
            "and (:startDate is null or t.moment >= :startDate) " +
            "and (:endDate is null or t.moment < :endDate) order by t.moment desc")
    Page<TransactionEntity> findStatement(@Param("accountId") Long accountId,
                                          @Param("type") TransactionType type,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);
}
