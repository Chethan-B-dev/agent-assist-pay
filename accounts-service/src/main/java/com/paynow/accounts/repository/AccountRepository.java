package com.paynow.accounts.repository;

import com.paynow.accounts.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for Account entities
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCustomerId(String customerId);

    /**
     * Find account by customer ID with pessimistic write lock for transactional safety
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.customerId = :customerId")
    Optional<Account> findByCustomerIdForUpdate(@Param("customerId") String customerId);

    boolean existsByCustomerId(String customerId);
}