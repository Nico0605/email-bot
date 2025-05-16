package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "yearly_futures")
public class Yearly extends PanacheEntityBase {
    @Id
    private Integer id;

    private Integer year;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    public Futures future;

    public Yearly() {}
    public Yearly(final Futures future, final Integer year) {
        this.future = future;
        this.year = year;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Futures getFuture() {
        return future;
    }

    public void setFuture(Futures future) {
        this.future = future;
    }

    @Override
    public String toString() {
        return "Yearly{" +
                "id=" + id +
                ", year=" + year +
                ", future=" + future +
                '}';
    }
}
