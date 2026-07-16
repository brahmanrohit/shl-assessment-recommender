package com.teamtask.repository;

import com.teamtask.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Data access for projects.
 *
 * Note the "join fetch p.owner" in the queries: it loads the owner in the
 * SAME SQL query (one query total). Without it, mapping each project to a
 * response that shows the owner's name would fire one extra SELECT per row -
 * the classic N+1 problem. Watch the SQL log to see the difference.
 */
@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {

    /** All projects, owner pre-loaded (ADMIN view). */
    public List<Project> listAllWithOwner() {
        return find("select p from Project p join fetch p.owner order by p.id").list();
    }

    /** Projects owned by one user, owner pre-loaded. */
    public List<Project> listByOwner(Long ownerId) {
        return find("select p from Project p join fetch p.owner o where o.id = ?1 order by p.id", ownerId)
                .list();
    }

    /** One project with its owner, or null. */
    public Project findByIdWithOwner(Long id) {
        return find("select p from Project p join fetch p.owner where p.id = ?1", id)
                .firstResult();
    }
}
