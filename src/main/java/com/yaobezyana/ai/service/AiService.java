package com.yaobezyana.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaobezyana.ai.config.OllamaProperties;
import com.yaobezyana.ai.dto.InboxSuggestionResponse;
import com.yaobezyana.ai.dto.SprintSuggestionsResponse;
import com.yaobezyana.ai.dto.SprintTaskSuggestion;
import com.yaobezyana.ai.exception.AiUnavailableException;
import com.yaobezyana.inbox.dto.ConversionType;
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

    private final RestClient ollamaRestClient;
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;

    // ─── Variant 1: Smart Inbox Suggestion ───────────────────────────────────

    public InboxSuggestionResponse suggestInboxConversion(String noteContent, List<ProjectInfo> projects) {
        assertEnabled();
        String json = callOllama(buildInboxPrompt(noteContent, projects));
        try {
            JsonNode root = objectMapper.readTree(json);
            ConversionType type = ConversionType.valueOf(root.path("type").asText("ROUTINE"));
            Long projectId = root.path("suggestedProjectId").isNull()
                    ? null : root.path("suggestedProjectId").asLong();
            return InboxSuggestionResponse.builder()
                    .type(type)
                    .suggestedTitle(root.path("suggestedTitle").asText())
                    .suggestedProjectId(projectId)
                    .reason(root.path("reason").asText())
                    .build();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("Failed to parse AI inbox suggestion: {}", json, e);
            throw new AiUnavailableException("ИИ вернул некорректный ответ, попробуйте ещё раз");
        }
    }

    // ─── Variant 2: Sprint Task Advisor ──────────────────────────────────────

    public SprintSuggestionsResponse suggestSprintTasks(List<AvailableProjectGroup> groups) {
        assertEnabled();
        String json = callOllama(buildSprintTasksPrompt(groups));
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

    // ─── Variant 3: Sprint Completion Summary ────────────────────────────────

    public String generateSprintSummary(List<CompletedTaskInfo> tasks, int sprintNumber) {
        if (!ollamaProperties.isEnabled()) return null;
        try {
            String json = callOllama(buildSprintSummaryPrompt(tasks, sprintNumber));
            JsonNode root = objectMapper.readTree(json);
            return root.path("summary").asText(null);
        } catch (Exception e) {
            log.warn("Sprint #{} summary generation failed: {}", sprintNumber, e.getMessage());
            return null;
        }
    }

    // ─── Prompt builders ─────────────────────────────────────────────────────

    private String buildInboxPrompt(String noteContent, List<ProjectInfo> projects) {
        StringBuilder sb = new StringBuilder();
        sb.append("Проанализируй заметку и предложи конвертацию.\n\n");
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

                Типы конвертации:
                - PROJECT: создать новый проект (только если это большая долгосрочная инициатива)
                - TASK: задача в существующем проекте (нужен suggestedProjectId из списка выше)
                - ROUTINE: разовое дело без проекта (Текучка)

                Ответь строго в JSON без пояснений:
                {"type":"...","suggestedTitle":"...","suggestedProjectId":null,"reason":"..."}

                Правила: type — одно из PROJECT/TASK/ROUTINE; suggestedTitle — кратко на языке заметки; suggestedProjectId — id из списка или null; reason — одно предложение.
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

    private String buildSprintSummaryPrompt(List<CompletedTaskInfo> tasks, int sprintNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("Напиши краткое резюме завершённого спринта.\n\n");
        sb.append("Спринт #").append(sprintNumber).append(". Выполненные задачи:\n");
        for (CompletedTaskInfo t : tasks) {
            String prefix = t.projectTitle() != null ? "[" + t.projectTitle() + "] " : "[Текучка] ";
            sb.append("- ").append(prefix).append(t.title()).append("\n");
        }
        sb.append("""

                Ответь строго в JSON без пояснений:
                {"summary":"..."}

                Резюме: 2-3 предложения на русском. Что было сделано, краткая оценка динамики.
                """);
        return sb.toString();
    }

    // ─── Ollama HTTP call ─────────────────────────────────────────────────────

    private String callOllama(String userPrompt) {
        try {
            OllamaChatRequest request = new OllamaChatRequest(
                    ollamaProperties.getModel(),
                    List.of(
                            new OllamaMessage("system", "Ты умный ассистент. Отвечай строго валидным JSON без пояснений."),
                            new OllamaMessage("user", userPrompt)
                    ),
                    false,
                    "json"
            );
            OllamaChatResponse response = ollamaRestClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);
            return response.message().content();
        } catch (RestClientException e) {
            log.error("Ollama unavailable: {}", e.getMessage());
            throw new AiUnavailableException("ИИ сервис недоступен. Убедитесь что Ollama запущена.");
        }
    }

    private void assertEnabled() {
        if (!ollamaProperties.isEnabled()) {
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

    public record CompletedTaskInfo(String title, String projectTitle) {}

    private record OllamaChatRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            String format
    ) {}

    private record OllamaMessage(String role, String content) {}

    private record OllamaChatResponse(OllamaMessage message) {}
}
