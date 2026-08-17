package com.beetloop.vendorproducts.pm.repository;

import com.beetloop.vendorproducts.pm.domain.PmIdSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PmIdSequenceRepository extends JpaRepository<PmIdSequence, PmIdSequence.Key> {

    /**
     * Locks the counter row so concurrent id requests serialise. Returns null on
     * first use for a (prefix, year) pair, in which case the caller creates it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PmIdSequence s WHERE s.prefix = :prefix AND s.year = :year")
    PmIdSequence lock(@Param("prefix") String prefix, @Param("year") int year);
}
