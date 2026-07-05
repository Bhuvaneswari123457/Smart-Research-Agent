package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    public String id;
    public String status;
    public String model;
    public List<Step> steps;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        public String type;
        public String signature;
        public List<ContentPart> content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentPart {
        public String type;
        public String text;
    }
}