package com.aws.carddemo.repository;

import com.aws.carddemo.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustSsn(String ssn);

    List<Customer> findByCustLastNameIgnoreCase(String lastName);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.custLastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByLastName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.custLastName) LIKE LOWER(CONCAT('%', :lastName, '%')) " +
           "AND LOWER(c.custFirstName) LIKE LOWER(CONCAT('%', :firstName, '%'))")
    List<Customer> searchByName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    Page<Customer> findByCustAddrStateCd(String stateCd, Pageable pageable);

    List<Customer> findByCustAddrZip(String zip);

    @Query("SELECT c FROM Customer c WHERE c.custFicoCreditScore >= :minScore")
    List<Customer> findByMinFicoScore(@Param("minScore") Integer minScore);

    @Query("SELECT c FROM Customer c WHERE c.custFicoCreditScore BETWEEN :minScore AND :maxScore")
    List<Customer> findByFicoScoreRange(@Param("minScore") Integer minScore, @Param("maxScore") Integer maxScore);

    @Query("SELECT c FROM Customer c WHERE c.custPriCardHolderInd = 'Y'")
    List<Customer> findPrimaryCardHolders();

    @Query("SELECT c FROM Customer c JOIN FETCH c.cardXrefs WHERE c.custId = :custId")
    Optional<Customer> findByIdWithCardXrefs(@Param("custId") Long custId);
}
