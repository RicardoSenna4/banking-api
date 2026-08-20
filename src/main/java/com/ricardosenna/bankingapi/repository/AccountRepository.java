package com.ricardosenna.bankingapi.repository;

import com.ricardosenna.bankingapi.entity.AccountEntity;
import com.ricardosenna.bankingapi.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByAccountNumber(Integer accountNumber);

    boolean existsByAccountNumber(Integer accountNumber);

    List<AccountEntity> findAllByClientId(Long clientId);

    Optional<AccountEntity> findByClientIdAndType(Long clientId, AccountType type);
}
