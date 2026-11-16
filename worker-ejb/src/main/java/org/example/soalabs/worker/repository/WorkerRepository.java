package org.example.soalabs.worker.repository;

import jakarta.persistence.EntityManager;
import org.example.soalabs.worker.domain.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import java.time.Instant;
import java.util.List;

// Called only inside a container-managed EJB transaction; no Spring context is started.
public class WorkerRepository extends SimpleJpaRepository<Worker, Integer> {
    private final EntityManager em;

    public WorkerRepository(EntityManager em) {
        super(Worker.class, em);
        this.em = em;
    }

    public Page<Worker> findByNameStartingWith(String prefix, Pageable pageable) {
        String escaped = prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return findAll((root, query, cb) -> cb.like(root.get("name"), escaped + "%", '\\'), pageable);
    }

    public long countByEndDate(Instant date) {
        return em.createQuery("select count(w) from Worker w where w.endDate = :date", Long.class)
                .setParameter("date", date).getSingleResult();
    }

    public long countByEndDateIsNull() {
        return em.createQuery("select count(w) from Worker w where w.endDate is null", Long.class).getSingleResult();
    }

    public long countBySalaryLessThan(Integer threshold) {
        return em.createQuery("select count(w) from Worker w where w.salary < :threshold", Long.class)
                .setParameter("threshold", threshold).getSingleResult();
    }

    public List<Object[]> countGroupedByEndDate() {
        return em.createQuery("select w.endDate, count(w) from Worker w group by w.endDate order by w.endDate",
                Object[].class).getResultList();
    }

    public Long sumSalaries() {
        return em.createQuery("select coalesce(sum(w.salary), 0) from Worker w", Long.class).getSingleResult();
    }
}
