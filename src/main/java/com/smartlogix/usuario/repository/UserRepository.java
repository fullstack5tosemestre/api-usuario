package com.smartlogix.usuario.repository;

import com.smartlogix.usuario.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad User.
 * Patrón de diseño: Repository — abstrae el acceso a datos.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRut(String rut);

    Optional<User> findByEmail(String email);

    List<User> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrRutContainingIgnoreCase(
            String nombre, String apellido, String rut);

    boolean existsByRut(String rut);

    boolean existsByEmail(String email);
}
