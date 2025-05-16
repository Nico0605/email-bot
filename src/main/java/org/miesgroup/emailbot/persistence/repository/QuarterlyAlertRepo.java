package org.miesgroup.emailbot.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.QuarterlyAlert;
import org.miesgroup.emailbot.persistence.repository.interfaces.GenericAlertRepo;

import java.util.Optional;

@ApplicationScoped
public class QuarterlyAlertRepo implements GenericAlertRepo<QuarterlyAlert, Long> {
    @Override
    @Transactional
    public Optional<QuarterlyAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }
}
