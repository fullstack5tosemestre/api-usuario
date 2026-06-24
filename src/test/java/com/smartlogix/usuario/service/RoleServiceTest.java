package com.smartlogix.usuario.service;

import com.smartlogix.usuario.model.Role;
import com.smartlogix.usuario.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void saveRoleReturnsSavedRole() {
        Role role = new Role(null, "ADMIN", "Administrador");
        Role saved = new Role(1L, "ADMIN", "Administrador");
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        Role result = roleService.save(role);

        assertNotNull(result.getId());
        assertEquals("ADMIN", result.getNombre());
    }

    @Test
    void findAllReturnsAllRoles() {
        List<Role> roles = List.of(
                new Role(1L, "ADMIN", "Admin"),
                new Role(2L, "CLIENTE", "Cliente")
        );
        when(roleRepository.findAll()).thenReturn(roles);

        List<Role> result = roleService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findByIdReturnsRoleWhenFound() {
        Role role = new Role(1L, "ADMIN", "Administrador");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        Optional<Role> result = roleService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getNombre());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.findById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void updateUpdatesRoleWhenExists() {
        Role role = new Role(1L, "ADMIN", "Administrador");
        when(roleRepository.existsById(1L)).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role result = roleService.update(1L, role);

        assertEquals(1L, result.getId());
        verify(roleRepository).save(role);
    }

    @Test
    void findByNombreReturnsRoleWhenFound() {
        Role role = new Role(1L, "ADMIN", "Administrador");
        when(roleRepository.findByNombre("ADMIN")).thenReturn(Optional.of(role));

        Optional<Role> result = roleService.findByNombre("ADMIN");

        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getNombre());
    }

    @Test
    void updateThrowsWhenRoleNotExists() {
        when(roleRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                roleService.update(99L, new Role(99L, "X", "Y")));
    }

    @Test
    void deleteThrowsWhenRoleNotExists() {
        when(roleRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> roleService.delete(99L));
    }

    @Test
    void deleteCallsRepositoryWhenExists() {
        when(roleRepository.existsById(1L)).thenReturn(true);

        roleService.delete(1L);

        verify(roleRepository).deleteById(1L);
    }
}
