package org.miesgroup.emailbot.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.miesgroup.emailbot.persistence.entity.Cliente;

@ApplicationScoped
public class ClienteRepo implements PanacheRepositoryBase<Cliente, Integer> {

}