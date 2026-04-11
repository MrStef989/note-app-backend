package com.yaobezyana.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на вход в систему")
public class LoginRequest {

    @NotBlank
    @Schema(description = "Email пользователя", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Schema(description = "Пароль", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
