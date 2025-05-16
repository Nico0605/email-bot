package org.miesgroup.emailbot.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.Futures;
import org.miesgroup.emailbot.persistence.entity.Monthly;
import org.miesgroup.emailbot.persistence.entity.Quarterly;
import org.miesgroup.emailbot.persistence.entity.Yearly;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class FuturesRepo implements PanacheRepository<Futures> {

    @Transactional
    public List<Yearly> findByYear(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Yearly.find("future.date", localDate).list();
    }

    @Transactional
    public List<Quarterly> findByQuarter(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Quarterly.find("future.date", localDate).list();
    }

    @Transactional
    public List<Monthly> findByMonth(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Monthly.find("future.date", localDate).list();
    }

    @Transactional
    public String getLastDateFromYearlyFutures() {
        return Yearly.findAll()
                .<Yearly>stream()
                .map(y -> y.getFuture().getDate())
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .map(LocalDate::toString)
                .orElse(null);
    }
}
