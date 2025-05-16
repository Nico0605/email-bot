package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "futures")
public class Futures extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Temporal(TemporalType.DATE)
    private LocalDate date;

    private Double settlementPrice;

    public Futures() {}

    public Futures(final LocalDate date, final Double settlementPrice) {}

    public Integer getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Double getSettlementPrice() {
        return settlementPrice;
    }
}