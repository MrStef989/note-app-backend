package ru.atc.university.noteapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class TestDataFactory {

    public static String toJson(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }

    public static Map<String, String> registerBody(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    public static Map<String, Object> sprintBody(String title) {
        return Map.of("title", title);
    }

    public static Map<String, Object> projectBody(String title, Long sprintId) {
        if (sprintId == null) return Map.of("title", title);
        return Map.of("title", title, "sprintId", sprintId);
    }

    public static Map<String, Object> taskBody(String title, Long projectId) {
        if (projectId == null) return Map.of("title", title);
        return Map.of("title", title, "projectId", projectId);
    }
}