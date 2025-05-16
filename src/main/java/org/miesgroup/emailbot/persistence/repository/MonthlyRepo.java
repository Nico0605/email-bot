package org.miesgroup.emailbot.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.miesgroup.emailbot.persistence.entity.Monthly;


@ApplicationScoped
public class MonthlyRepo implements PanacheRepository<Monthly> {
}
