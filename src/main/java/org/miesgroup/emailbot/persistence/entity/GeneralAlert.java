package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "GeneralAlert")
public class GeneralAlert extends PanacheEntityBase {

    @Id
    @Column(name = "Id_Utente")
    private Integer idUtente; // usato come PK

    private Double maxPriceValue;
    private Double minPriceValue;
    private Boolean checkModality;

    @ManyToOne
    @JoinColumn(name = "Id_Utente", insertable = false, updatable = false)
    private Cliente utente;

    public GeneralAlert() {}

    public GeneralAlert(Double maxPriceValue, Double minPriceValue, Integer idUtente, Boolean checkModality) {
        this.maxPriceValue = maxPriceValue;
        this.minPriceValue = minPriceValue;
        this.idUtente = idUtente;
        this.checkModality = checkModality;
    }

    public Integer getIdUtente() {
        return idUtente;
    }

    public Double getMaxPriceValue(){
        return maxPriceValue;
    }

    public Double getMinPriceValue(){
        return minPriceValue;
    }

    public Boolean getCheckModality(){
        return checkModality;
    }

    public Cliente getUtente(){
        return utente;
    }
}
