package org.miesgroup.emailbot.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;


@Entity
@Table(name = "utente")
public class Cliente extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_Utente")
    private Integer id;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Sede_Legale")
    private String sedeLegale;

    @Column(name = "Piva", length = 11)
    private String pIva;

    @Column(name = "Email")
    private String email;

    @Column(name = "Telefono")
    private String telefono;

    @Column(name = "Stato")
    private String stato;

    @Column(name = "Tipologia", nullable = false)
    private String tipologia = "Cliente"; // Default impostato nel DB

    @Column(name = "Classe_Agevolazione")
    private String classeAgevolazione;

    @Column(name = "codice_ateco")
    private String codiceAteco;

    @Column(name = "energivori")
    private Boolean energivori = false; // Default impostato nel DB

    @Column(name = "gassivori")
    private Boolean gassivori = false; // Default impostato nel DB

    @Column(name = "consumo_annuo")
    private Float consumoAnnuoEnergia;

    @Column(name = "fatturato_annuo")
    private Float fatturatoAnnuo;

    @Column(name = "checkEmail")
    private Boolean checkEmail;

    public Cliente() {
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSedeLegale() {
        return sedeLegale;
    }

    public String getpIva() {
        return pIva;
    }

    public String getStato() {
        return stato;
    }

    public String getTipologia() {
        return tipologia;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getClasseAgevolazione() {
        return classeAgevolazione;
    }

    public String getCodiceAteco() {
        return codiceAteco;
    }

    public Boolean getEnergivori() {
        return energivori;
    }

    public Boolean getCheckEmail() {
        return checkEmail;
    }

    public void setCheckEmail(boolean checkEmail) {
        this.checkEmail = checkEmail;
    }

    public Boolean isGassivori() {
        return gassivori;
    }

    public Float getConsumoAnnuoEnergia() {
        return consumoAnnuoEnergia;
    }

    public Float getFatturatoAnnuo() {
        return fatturatoAnnuo;
    }

}
