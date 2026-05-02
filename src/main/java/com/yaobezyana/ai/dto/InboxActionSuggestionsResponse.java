package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Список предложений ИИ по обработке заметки из инбокса")
public class InboxActionSuggestionsResponse {

    @Schema(description = "Список возможных действий (обычно 2-3 варианта)")
    private List<InboxActionSuggestion> actions;
}
