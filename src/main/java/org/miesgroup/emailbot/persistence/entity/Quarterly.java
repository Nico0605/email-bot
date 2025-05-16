package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "quarterly_futures")
public class Quarterly extends PanacheEntityBase {
    @Id
    private Integer id;

    private Integer year;

    private Integer quarter;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    public Futures future;

    public Quarterly() {}

    public Quarterly(final Futures future, final Integer year, final Integer quarter) {
        this.future = future;
        this.year = year;
        this.quarter = quarter;
    }

    public Integer getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public Futures getFuture() {
        return future;
    }
}
