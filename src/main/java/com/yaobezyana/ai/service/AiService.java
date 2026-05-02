package com.yaobezyana.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaobezyana.ai.config.AiProperties;
import com.yaobezyana.ai.dto.*;
import com.yaobezyana.ai.exception.AiUnavailableException;
import com.yaobezyana.sprint.dto.AvailableProjectGroup;
import com.yaobezyana.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestClient aiRestClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    // ─── Variant 1: Smart Inbox Actions ──────────────────────────────────────

    public InboxActionSuggestionsResponse suggestInboxActions(String noteContent, List<ProjectInfo> projects) {
        assertEnabled();
        String json = callGroq(buildInboxActionsPrompt(noteContent, projects));
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode actionsNode = root.path("actions");
            List<InboxActionSuggestion> actions = new ArrayList<>();
            if (actionsNode.isArray()) {
                for (JsonNode node : actionsNode) {
                    InboxAction action;
                    try {
                        action = InboxAction.valueOf(node.path("action").asText("CREATE_TASK"));
                    } catch (IllegalArgumentException e) {
                        action = InboxAction.CREATE_TASK;
                    }
                    Long projectId = node.path("projectId").isNull() ? null : node.path("projectId").asLong();
                    String projectTitle = node.path("projectTitle").isNull() ? null : node.path("projectTitle").asText(null);
                    actions.add(InboxActionSuggestion.builder()
                            .action(action)
                            .title(node.path("title").asText(""))
                            .projectId(projectId)
                            .projectTitle(projectTitle)
                            .reason(node.path("reason").asText(""))
                            .build());
                }
            }
            return InboxActionSuggestionsResponse.builder().actions(actions).build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI inbox actions: {}", json, e);
            throw new AiUnavailableException("ИИ вернул некорректный ответ, попробуйте ещё раз");
        }
    }

    // ─── Variant 2: Sprint Task Advisor ──────────────────────────────────────

    public SprintSuggestionsResponse suggestSprintTasks(List<AvailableProjectGroup> groups) {
        assertEnabled();
        String json = callGroq(buildSprintTasksPrompt(groups));
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode suggestionsNode = root.path("suggestions");
            List<SprintTaskSuggestion> suggestions = new ArrayList<>();
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    Long projectId = node.path("projectId").isNull()
                            ? null : node.path("projectId").asLong();
                    Long taskId = node.path("taskId").asLong();
                    String reason = node.path("reason").asText();
                    suggestions.add(SprintTaskSuggestion.builder()
                            .projectId(projectId)
                            .projectTitle(resolveProjectTitle(groups, projectId))
                            .taskId(taskId)
                            .taskTitle(resolveTaskTitle(groups, projectId, taskId))
                            .reason(reason)
                            .build());
                }
            }
            return SprintSuggestionsResponse.builder().suggestions(suggestions).build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI sprint suggestions: {}", json, e);
            throw new AiUnavailableException("ИИ вернул некорректный ответ, попробуйте ещё раз");
        }
    }

    // ─── Variant 3: Project Description Autocomplete ─────────────────────────

    public AutocompleteResponse autocompleteProjectText(String projectTitle, String currentText) {
        assertEnabled();
        String json = callGroq(buildAutocompletePrompt(projectTitle, currentText));
        try {
            JsonNode root = objectMapper.readTree(json);
            return AutocompleteResponse.builder()
                    .suggestion(root.path("suggestion").asText(""))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI autocomplete: {}", json, e);
            throw new AiUnavailableException("ИИ вернул некорректный ответ");
        }
    }

    // ─── Prompt builders ─────────────────────────────────────────────────────

    private String buildInboxActionsPrompt(String noteContent, List<ProjectInfo> projects) {
        StringBuilder sb = new StringBuilder();
        sb.append("Проанализируй заметку и предложи 2-3 действия для её обработки.\n\n");
        sb.append("Заметка: \"").append(noteContent).append("\"\n\n");
        if (!projects.isEmpty()) {
            sb.append("Проекты пользователя:\n");
            for (ProjectInfo p : projects) {
                sb.append("- id=").append(p.id()).append(", название=\"").append(p.title()).append("\"\n");
            }
        } else {
            sb.append("Проектов пока нет.\n");
        }
        sb.append("""

                Типы действий:
                - CREATE_TASK: создать задачу в существующем проекте (нужен projectId из списка выше)
                - CREATE_PROJECT: создать новый проект (только для крупной долгосрочной инициативы)
                - ADD_TO_CALENDAR: добавить напоминание/блокировку в календарь (если есть дата или ожидание)
                - UPDATE_REFERENCE: обновить базу знаний/справочную (если заметка содержит полезную информацию о процессах, людях, технологиях)

                Предложи наиболее подходящие действия. Для CREATE_TASK укажи projectId и projectTitle из списка выше.

                Ответь строго в JSON без пояснений:
                {"actions":[{"action":"CREATE_TASK","title":"...","projectId":2,"projectTitle":"Backend","reason":"..."},{"action":"UPDATE_REFERENCE","title":"...","projectId":null,"projectTitle":null,"reason":"..."}]}

                Правила: action — одно из CREATE_TASK/CREATE_PROJECT/ADD_TO_CALENDAR/UPDATE_REFERENCE; title — кратко на языке заметки; reason — одно предложение.
                """);
        return sb.toString();
    }

    private String buildSprintTasksPrompt(List<AvailableProjectGroup> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append("Предложи по одной приоритетной задаче из каждой группы для включения в спринт.\n\n");
        for (AvailableProjectGroup group : groups) {
            if (group.getProjectId() != null) {
                sb.append("Проект \"").append(group.getProjectTitle())
                  .append("\" (projectId=").append(group.getProjectId()).append("):\n");
            } else {
                sb.append("Текучка (projectId=null):\n");
            }
            for (TaskResponse task : group.getTasks()) {
                sb.append("  - taskId=").append(task.getId())
                  .append(", \"").append(task.getTitle()).append("\"\n");
            }
            sb.append("\n");
        }
        sb.append("""
                Выбери из каждой группы одну задачу. Приоритет: конкретные задачи важнее расплывчатых.

                Ответь строго в JSON без пояснений:
                {"suggestions":[{"projectId":1,"taskId":5,"reason":"..."},{"projectId":null,"taskId":10,"reason":"..."}]}
                """);
        return sb.toString();
    }

    private String buildAutocompletePrompt(String projectTitle, String currentText) {
        return """
                Ты помогаешь пользователю описать проект. Продолжи текст описания.

                Название проекта: "%s"
                Текст уже написан: "%s"

                Предложи естественное продолжение (1-2 предложения), не повторяй написанное.

                Ответь строго в JSON без пояснений:
                {"suggestion":"..."}
                """.formatted(projectTitle, currentText);
    }

    // ─── Groq API call ───────────────────────────────────────────────────────

    private String callGroq(String userPrompt) {
        try {
            GroqRequest request = new GroqRequest(
                    aiProperties.getModel(),
                    List.of(
                            new GroqMessage("system", "Ты умный ассистент. Отвечай строго валидным JSON без пояснений."),
                            new GroqMessage("user", userPrompt)
                    ),
                    new ResponseFormat("json_object")
            );
            GroqResponse response = aiRestClient.post()
                    .uri("/openai/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(GroqResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiUnavailableException("ИИ вернул пустой ответ");
            }
            return response.choices().get(0).message().content();
        } catch (RestClientException e) {
            log.error("Groq API unavailable: {}", e.getMessage());
            throw new AiUnavailableException("ИИ сервис недоступен. Проверьте GROQ_API_KEY.");
        }
    }

    private void assertEnabled() {
        if (!aiProperties.isEnabled()) {
            throw new AiUnavailableException("AI функции отключены (AI_ENABLED=false)");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String resolveProjectTitle(List<AvailableProjectGroup> groups, Long projectId) {
        return groups.stream()
                .filter(g -> projectId == null ? g.getProjectId() == null : projectId.equals(g.getProjectId()))
                .map(AvailableProjectGroup::getProjectTitle)
                .findFirst().orElse(null);
    }

    private String resolveTaskTitle(List<AvailableProjectGroup> groups, Long projectId, Long taskId) {
        return groups.stream()
                .filter(g -> projectId == null ? g.getProjectId() == null : projectId.equals(g.getProjectId()))
                .flatMap(g -> g.getTasks().stream())
                .filter(t -> taskId.equals(t.getId()))
                .map(TaskResponse::getTitle)
                .findFirst().orElse(null);
    }

    // ─── Inner types ──────────────────────────────────────────────────────────

    public record ProjectInfo(Long id, String title) {}

    private record GroqRequest(
            String model,
            List<GroqMessage> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {}

    private record GroqMessage(String role, String content) {}

    private record ResponseFormat(String type) {}

    private record GroqResponse(List<GroqChoice> choices) {}

    private record GroqChoice(GroqMessage message) {}
}
