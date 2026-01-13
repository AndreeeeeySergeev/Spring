package com.mephi.task.booking.web;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;

/**
 * MockMvc тесты для AuthController
 * Проверяют регистрацию и авторизацию пользователей
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Регистрация нового пользователя возвращает JWT токен")
    void register_newUser_returnsToken() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "newuser");
        request.put("password", "password123");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("Регистрация с пустым username возвращает 400")
    void register_emptyUsername_returnsBadRequest() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "");
        request.put("password", "password123");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Авторизация с правильными credentials возвращает токен")
    void login_validCredentials_returnsToken() throws Exception {
        // Подготовка: создаем пользователя
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(encoder.encode("testpass"));
        user.setRole("USER");
        userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "testpass");

        mockMvc.perform(post("/api/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("Авторизация с неправильным паролем возвращает 401")
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        // Подготовка: создаем пользователя
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(encoder.encode("correctpass"));
        user.setRole("USER");
        userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "wrongpass");

        mockMvc.perform(post("/api/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Авторизация несуществующего пользователя возвращает 401")
    void login_nonExistentUser_returnsUnauthorized() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "nonexistent");
        request.put("password", "anypass");

        mockMvc.perform(post("/api/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Регистрация создает пользователя с ролью USER")
    void register_createsUserWithUserRole() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "roletest");
        request.put("password", "pass123");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User saved = userRepository.findByUsername("roletest").orElseThrow();
        assert saved.getRole().equals("USER");
    }
}

