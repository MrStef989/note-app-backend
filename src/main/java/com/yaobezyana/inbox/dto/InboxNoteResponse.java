package com.yaobezyana.inbox.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Заметка в инбоксе")
public class InboxNoteResponse {

    @Schema(description = "Идентификатор заметки", example = "1")
    private Long id;

    @Schema(description = "Содержимое заметки", example = "Разобраться с CI/CD")
    private String content;

    @Schema(description = "Дата создания", example = "2026-04-25T10:00:00")
    private LocalDateTime createdAt;
}
