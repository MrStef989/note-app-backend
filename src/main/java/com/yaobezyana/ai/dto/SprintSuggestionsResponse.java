package com.yaobezyana.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Список предложений ИИ: по одной задаче на каждый проект/группу")
public class SprintSuggestionsResponse {

    @Schema(description = "Предложения (одно на группу проекта или Текучки)")
    private List<SprintTaskSuggestion> suggestions;
}
