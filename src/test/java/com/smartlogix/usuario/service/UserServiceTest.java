package com.smartlogix.usuario.service;

import com.smartlogix.usuario.dto.OrderDTO;
import com.smartlogix.usuario.dto.ProductDTO;
import com.smartlogix.usuario.model.Role;
import com.smartlogix.usuario.model.User;
import com.smartlogix.usuario.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserService userService;

    private User sampleUser(Long id, String nombre, String apellido, String rut, String email) {
        Role role = new Role(1L, "CLIENTE", "Cliente");
        return new User(id, nombre, apellido, rut, email,
                "$2a$10$hash", LocalDate.of(1990, 1, 1),
                "Calle Falsa 123", null, role);
    }

    @Test
    void saveUserSetsRegistrationDate() {
        User user = sampleUser(null, "Carlos", "González", "12345678-9", "carlos@test.cl");
        User saved = sampleUser(1L, "Carlos", "González", "12345678-9", "carlos@test.cl");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.save(user);

        verify(userRepository).save(any(User.class));
        assertNotNull(result.getId());
    }

    @Test
    void findAllReturnsAllUsers() {
        List<User> users = List.of(
                sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl"),
                sampleUser(2L, "Luis", "Pérez", "22222222-2", "luis@test.cl")
        );
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findByIdReturnsUserWhenFound() {
        User user = sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Ana", result.get().getNombre());
    }

    @Test
    void findByRutReturnsUserWhenFound() {
        User user = sampleUser(1L, "Carlos", "González", "12345678-9", "carlos@test.cl");
        when(userRepository.findByRut("12345678-9")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByRut("12345678-9");

        assertTrue(result.isPresent());
        assertEquals("12345678-9", result.get().getRut());
    }

    @Test
    void findByEmailReturnsUserWhenFound() {
        User user = sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl");
        when(userRepository.findByEmail("ana@test.cl")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("ana@test.cl");

        assertTrue(result.isPresent());
        assertEquals("ana@test.cl", result.get().getEmail());
    }

    @Test
    void searchByNameDelegatesToRepository() {
        List<User> users = List.of(sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl"));
        when(userRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase("ana", "ana"))
                .thenReturn(users);

        List<User> result = userService.searchByName("ana");

        assertEquals(1, result.size());
    }

    @Test
    void updateUpdatesUserWhenExists() {
        User existing = sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl");
        User incoming = sampleUser(null, "Ana", "Torres Editado", "11111111-1", "ana@test.cl");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.update(1L, incoming);

        assertEquals(1L, result.getId());
        assertEquals("Torres Editado", result.getApellido());
        verify(userRepository).save(incoming);
    }

    @Test
    void updateThrowsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userService.update(99L, sampleUser(99L, "X", "Y", "1-1", "x@test.cl")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrdersByUserFiltersByCustomerName() {
        User user = sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        OrderDTO matching = new OrderDTO(1L, "Ana Torres", "PENDIENTE", LocalDateTime.now());
        OrderDTO notMatching = new OrderDTO(2L, "Otro Cliente", "PENDIENTE", LocalDateTime.now());
        ReflectionTestUtils.setField(userService, "pedidosApiUrl", "http://gateway/api/v1/orders");
        when(restTemplate.exchange(
                eq("http://gateway/api/v1/orders"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(matching, notMatching)));

        List<OrderDTO> result = userService.getOrdersByUser(1L);

        assertEquals(1, result.size());
        assertEquals("Ana Torres", result.get(0).getCustomerName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrdersByUserReturnsEmptyWhenRestTemplateThrows() {
        User user = sampleUser(1L, "Ana", "Torres", "11111111-1", "ana@test.cl");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ReflectionTestUtils.setField(userService, "pedidosApiUrl", "http://gateway/api/v1/orders");
        when(restTemplate.exchange(
                eq("http://gateway/api/v1/orders"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("connection refused"));

        List<OrderDTO> result = userService.getOrdersByUser(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrdersByUserThrowsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getOrdersByUser(99L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCatalogoReturnsProductsFromRestTemplate() {
        ReflectionTestUtils.setField(userService, "inventarioApiUrl", "http://gateway/api/v1/products");
        ProductDTO product = new ProductDTO(1L, "Notebook", "SKU-1", 10);
        when(restTemplate.exchange(
                eq("http://gateway/api/v1/products"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(product)));

        List<ProductDTO> result = userService.getCatalogo();

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCatalogoReturnsEmptyWhenRestTemplateThrows() {
        ReflectionTestUtils.setField(userService, "inventarioApiUrl", "http://gateway/api/v1/products");
        when(restTemplate.exchange(
                eq("http://gateway/api/v1/products"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("connection refused"));

        List<ProductDTO> result = userService.getCatalogo();

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteThrowsWhenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.delete(99L));
    }

    @Test
    void deleteCallsRepositoryWhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }
}
