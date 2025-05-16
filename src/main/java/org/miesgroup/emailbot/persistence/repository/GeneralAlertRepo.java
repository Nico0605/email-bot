package org.miesgroup.emailbot.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.Cliente;
import org.miesgroup.emailbot.persistence.entity.GeneralAlert;

import java.util.Optional;

@ApplicationScoped
public class GeneralAlertRepo implements PanacheRepositoryBase<GeneralAlert, Long> {
    @Transactional
    public Optional<GeneralAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }
}