package org.miesgroup.emailbot.persistence.repository.interfaces;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.transaction.Transactional;
import java.util.Optional;

    /**
     * Generic repository interface for all alert types.
     * @param <T> The alert entity type
     * @param <ID> The primary key type
     */
public interface GenericAlertRepo<T, ID> extends PanacheRepositoryBase<T, ID> {
    /**
     * Finds an alert by user ID.
     * @param userId The user ID
     * @return Optional containing the alert if found, empty otherwise
     */
    @Transactional
    Optional<T> findByUserId(int userId);
}
