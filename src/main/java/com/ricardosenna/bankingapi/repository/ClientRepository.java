package com.ricardosenna.bankingapi.repository;

import com.ricardosenna.bankingapi.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    Optional<ClientEntity> findByCpf(String cpf);

    Optional<ClientEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByCpf(String cpf);

    Page<ClientEntity> findAll(Pageable pageable);
}
