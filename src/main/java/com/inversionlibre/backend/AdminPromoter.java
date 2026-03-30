package com.inversionlibre.backend;

import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminPromoter implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== BUSCANDO USUARIOS PARA HACER ADMIN ===");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            System.out.println("Encontrado: " + user.getEmail() + " | Rol actual: " + user.getRole());
            if (user.getRole() != User.Role.ADMIN) {
                user.setRole(User.Role.ADMIN);
                userRepository.save(user);
                System.out.println("-> ¡Promovido a ADMIN!");
            }
        }
        System.out.println("=========================================");
    }
}
