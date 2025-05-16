package org.miesgroup.emailbot.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.Cliente;
import org.miesgroup.emailbot.persistence.entity.MonthlyAlert;
import org.miesgroup.emailbot.persistence.entity.QuarterlyAlert;
import org.miesgroup.emailbot.persistence.repository.interfaces.GenericAlertRepo;

import java.util.Optional;

@ApplicationScoped
public class MonthlyAlertRepo implements GenericAlertRepo<MonthlyAlert, Long> {
    @Override
    @Transactional
    public Optional<MonthlyAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }
}