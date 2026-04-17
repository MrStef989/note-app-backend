package com.yaobezyana.sprint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Запрос на переупорядочивание задач внутри проекта")
public class ReorderRequest {

    @NotEmpty
    @Schema(description = "ID задач в желаемом порядке. Индекс в списке = позиция (0-based)",
            example = "[3, 1, 5, 2]")
    private List<Long> taskIds;
}
