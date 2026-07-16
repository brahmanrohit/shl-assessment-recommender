package com.teamtask.repository;

import com.teamtask.exception.InvalidQueryParameterException;
import com.teamtask.model.Project;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/**
 * Data access for projects.
 *
 * "join fetch p.owner" loads the owner in the SAME SQL query (the DTO shows
 * the owner's name - without the fetch that's an N+1). Fetching a *-to-one
 * relation is pagination-safe: it never multiplies rows.
 *
 * Sorting goes through a whitelist, same rules as TaskRepository.
 */
@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {

    private static final String LIST_BASE =
            "select p from Project p join fetch p.owner o";

    private static final Map<String, String> SORTABLE = Map.of(
            "id", "p.id",
            "name", "p.name",
            "createdAt", "p.createdAt");

    /** Page of all projects (ADMIN view). */
    public PanacheQuery<Project> queryAll(String sortField, boolean ascending) {
        return find(LIST_BASE + orderBy(sortField, ascending), Map.of());
    }

    /** Page of one user's projects. */
    public PanacheQuery<Project> queryByOwner(Long ownerId, String sortField, boolean ascending) {
        return find(LIST_BASE + " where o.id = :ownerId" + orderBy(sortField, ascending),
                Map.of("ownerId", ownerId));
    }

    public long countByOwner(Long ownerId) {
        return count("owner.id = ?1", ownerId);
    }

    /** One project with its owner, or null. */
    public Project findByIdWithOwner(Long id) {
        return find("select p from Project p join fetch p.owner where p.id = ?1", id)
                .firstResult();
    }

    private String orderBy(String sortField, boolean ascending) {
        String path = SORTABLE.get(sortField);
        if (path == null) {
            throw new InvalidQueryParameterException(
                    "Cannot sort projects by '" + sortField + "'. Allowed: " + String.join(", ", SORTABLE.keySet()));
        }
        return " order by " + path + (ascending ? " asc" : " desc") + ", p.id";
    }
}
