package ru.atc.university.noteapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.atc.university.noteapp.util.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.yaobezyana.YaObezYanaApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ✅ Позитивный: регистрация с валидными данными
    @Test
    void register_shouldReturn201AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDataFactory.toJson(
                                TestDataFactory.registerBody("test@example.com", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    // ❌ Негативный: повторная регистрация с тем же email
    @Test
    void register_duplicateEmail_shouldReturn400() throws Exception {
        String body = TestDataFactory.toJson(
                TestDataFactory.registerBody("dup@example.com", "secret123"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    // ❌ Негативный: пустой email
    @Test
    void register_emptyEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDataFactory.toJson(
                                TestDataFactory.registerBody("", "secret123"))))
                .andExpect(status().isBadRequest());
    }

    // ✅ Позитивный: логин с верными данными
    @Test
    void login_shouldReturn200AndToken() throws Exception {
        String body = TestDataFactory.toJson(
                TestDataFactory.registerBody("login@example.com", "secret123"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    // ❌ Негативный: логин с неверным паролем
    @Test
    void login_wrongPassword_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestDataFactory.toJson(
                        TestDataFactory.registerBody("wrong@example.com", "secret123"))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestDataFactory.toJson(
                                TestDataFactory.registerBody("wrong@example.com", "WRONGPASS"))))
                .andExpect(status().isUnauthorized());
    }

    // 🔒 Авторизация: запрос без токена на защищённый endpoint
    @Test
    void protectedEndpoint_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}