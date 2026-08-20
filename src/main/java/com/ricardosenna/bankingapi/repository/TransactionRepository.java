package com.ricardosenna.bankingapi.repository;

import com.ricardosenna.bankingapi.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Page<TransactionEntity> findAllByAccountId(Long accountId, Pageable pageable);

    Page<TransactionEntity> findAllByAccountIdOrderByMomentDesc(Long accountId, Pageable pageable);
}
