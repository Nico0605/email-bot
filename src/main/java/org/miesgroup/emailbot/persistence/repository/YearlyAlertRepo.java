package org.miesgroup.emailbot.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.YearlyAlert;
import org.miesgroup.emailbot.persistence.repository.interfaces.GenericAlertRepo;

import java.util.Optional;

@ApplicationScoped
public class YearlyAlertRepo implements GenericAlertRepo<YearlyAlert, Long> {
    @Override
    @Transactional
    public Optional<YearlyAlert> findByUserId(int userId) {
        return find("utente.id", userId).firstResultOptional();
    }
}