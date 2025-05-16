package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "YearlyAlert")
public class YearlyAlert extends PanacheEntityBase {

    @Id
    @Column(name = "Id_Utente")
    private Integer idUtente;

    private Double maxPriceValue;
    private Double minPriceValue;
    private Boolean checkModality;

    @ManyToOne
    @JoinColumn(name = "Id_Utente", insertable = false, updatable = false)
    private Cliente utente;

    public YearlyAlert() {}

    public YearlyAlert(Double maxPriceValue, Double minPriceValue, Integer idUtente, String frequencyA, Boolean checkModality) {
        this.maxPriceValue = maxPriceValue;
        this.minPriceValue = minPriceValue;
        this.idUtente = idUtente;
        this.checkModality = checkModality;
    }

    public Double getMaxPriceValue(){
        return maxPriceValue;
    }

    public Double getMinPriceValue(){
        return minPriceValue;
    }

    public Integer getIdUtente(){
        return idUtente;
    }

    public Boolean getCheckModality(){
        return checkModality;
    }

    public Cliente getUtente(){
        return utente;
    }
}
