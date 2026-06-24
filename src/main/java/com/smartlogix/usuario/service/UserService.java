package com.smartlogix.usuario.service;

import com.smartlogix.usuario.dto.OrderDTO;
import com.smartlogix.usuario.dto.ProductDTO;
import com.smartlogix.usuario.model.User;
import com.smartlogix.usuario.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la entidad User.
 * Patrón de diseño: Service Layer + Facade — centraliza la lógica de negocio
 * y la comunicación con otros microservicios (api-pedidos, api-inventario).
 *
 * Comunicación inter-servicios:
 * - api-pedidos → pedidos asociados al nombre del usuario
 * - api-inventario → catálogo de productos disponibles
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gateway.api.url}/api/v1/orders")
    private String pedidosApiUrl;

    @Value("${gateway.api.url}/api/v1/products")
    private String inventarioApiUrl;

    // ===================== CRUD =====================

    public User save(User user) {
        user.setFechaRegistro(LocalDateTime.now());
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /** Búsqueda por RUT (atributo de negocio, no ID de tabla) */
    public Optional<User> findByRut(String rut) {
        return userRepository.findByRut(rut);
    }

    /** Búsqueda por email */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /** Búsqueda por nombre, apellido o RUT (parcial, case-insensitive) */
    public List<User> searchByName(String query) {
        return userRepository
                .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrRutContainingIgnoreCase(
                        query, query, query);
    }

    public User update(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario con ID " + id + " no encontrado."));
        user.setId(id);
        user.setFechaRegistro(existing.getFechaRegistro());
        return userRepository.save(user);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario con ID " + id + " no encontrado.");
        }
        userRepository.deleteById(id);
    }

    // ===================== Comunicación inter-microservicios =====================

    /**
     * Obtiene todos los pedidos desde api-pedidos y filtra por el nombre
     * completo del usuario. Comunicación REST síncrona con RestTemplate.
     */
    public List<OrderDTO> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario con ID " + userId + " no encontrado."));

        String customerName = (user.getNombre() + " " + user.getApellido()).toLowerCase();

        try {
            ResponseEntity<List<OrderDTO>> response = restTemplate.exchange(
                    pedidosApiUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<OrderDTO>>() {
                    });

            List<OrderDTO> allOrders = response.getBody();
            if (allOrders == null)
                return Collections.emptyList();

            // Filtrar por customerName del usuario
            return allOrders.stream()
                    .filter(o -> o.getCustomerName() != null &&
                            o.getCustomerName().toLowerCase().contains(customerName))
                    .toList();

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene el catálogo de productos disponibles desde api-inventario.
     * Permite que el frontend muestre productos al usuario logueado.
     */
    public List<ProductDTO> getCatalogo() {
        try {
            ResponseEntity<List<ProductDTO>> response = restTemplate.exchange(
                    inventarioApiUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ProductDTO>>() {
                    });
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
