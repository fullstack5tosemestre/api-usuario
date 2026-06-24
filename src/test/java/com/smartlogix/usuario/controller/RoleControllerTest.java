package com.smartlogix.usuario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.usuario.model.Role;
import com.smartlogix.usuario.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
    }

    @Test
    void getAllReturnsOkWhenRolesExist() throws Exception {
        List<Role> roles = List.of(new Role(1L, "ADMIN", "Admin"));
        when(roleService.findAll()).thenReturn(roles);

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("ADMIN"));
    }

    @Test
    void getAllReturnsNoContentWhenEmpty() throws Exception {
        when(roleService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getByIdReturnsOkWhenFound() throws Exception {
        when(roleService.findById(1L)).thenReturn(Optional.of(new Role(1L, "ADMIN", "Admin")));

        mockMvc.perform(get("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        when(roleService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByNombreReturnsOkWhenFound() throws Exception {
        when(roleService.findByNombre("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN", "Admin")));

        mockMvc.perform(get("/api/v1/roles/by-nombre/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ADMIN"));
    }

    @Test
    void getByNombreReturnsNotFoundWhenMissing() throws Exception {
        when(roleService.findByNombre("X")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/roles/by-nombre/X"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsOkWhenFound() throws Exception {
        Role request = new Role(1L, "ADMIN", "Administrador Editado");
        when(roleService.update(eq(1L), any(Role.class))).thenReturn(request);

        mockMvc.perform(put("/api/v1/roles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Administrador Editado"));
    }

    @Test
    void updateReturnsNotFoundWhenServiceThrows() throws Exception {
        Role request = new Role(99L, "X", "Y");
        when(roleService.update(eq(99L), any(Role.class)))
                .thenThrow(new RuntimeException("not found"));

        mockMvc.perform(put("/api/v1/roles/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturnsCreated() throws Exception {
        Role request = new Role(null, "VENDEDOR", "Vendedor");
        Role saved = new Role(3L, "VENDEDOR", "Vendedor");
        when(roleService.save(any(Role.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isNoContent());

        verify(roleService).delete(1L);
    }

    @Test
    void deleteReturnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new RuntimeException("not found")).when(roleService).delete(99L);

        mockMvc.perform(delete("/api/v1/roles/99"))
                .andExpect(status().isNotFound());
    }
}
