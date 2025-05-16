package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "monthly_futures")
public class Monthly extends PanacheEntityBase {
    @Id
    private Integer id; // 🔹 Ora è private

    private Integer year;
    private Integer month;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Futures future;

    public Monthly(){}

    public Monthly(final Futures future, final Integer year, final Integer month){
        this.future = future;
        this.year = year;
        this.month = month;
    }

    public Integer getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Futures getFuture() {
        return future;
    }
}
