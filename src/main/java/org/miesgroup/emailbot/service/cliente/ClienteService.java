package org.miesgroup.emailbot.service.cliente;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.miesgroup.emailbot.persistence.entity.Cliente;
import org.miesgroup.emailbot.persistence.repository.ClienteRepo;

import java.util.List;

@ApplicationScoped
public class ClienteService {
    private final ClienteRepo clienteRepo;

    public ClienteService(ClienteRepo clienteRepo) {
        this.clienteRepo = clienteRepo;
    }

    @Transactional
    public List<Cliente> getClientsCheckEmail() {
        return clienteRepo.find("checkEmail", true).list();
    }
}
