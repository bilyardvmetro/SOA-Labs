package org.example.soalabs.worker.repository;

import org.example.soalabs.worker.domain.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface WorkerRepository extends JpaRepository<Worker, Integer>, JpaSpecificationExecutor<Worker> {

    Page<Worker> findByNameStartingWith(String prefix, Pageable pageable);

    long countByEndDate(Instant endDate);

    long countByEndDateIsNull();

    long countBySalaryLessThan(Integer threshold);

    @Query("select w.endDate, count(w) from Worker w group by w.endDate order by w.endDate")
    List<Object[]> countGroupedByEndDate();

    @Query("select coalesce(sum(w.salary), 0) from Worker w")
    Long sumSalaries();
}
