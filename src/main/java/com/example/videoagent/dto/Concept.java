package com.example.videoagent.dto;

/**
 * 知识点数据传输对象
 */
public class Concept {

    private String timestampFrom;
    private String timestampTo;
    private String concept;
    private String description;

    public Concept() {}

    public Concept(String timestampFrom, String timestampTo, String concept, String description) {
        this.timestampFrom = timestampFrom;
        this.timestampTo = timestampTo;
        this.concept = concept;
        this.description = description;
    }

    public String getTimestampFrom() {
        return timestampFrom;
    }

    public void setTimestampFrom(String timestampFrom) {
        this.timestampFrom = timestampFrom;
    }

    public String getTimestampTo() {
        return timestampTo;
    }

    public void setTimestampTo(String timestampTo) {
        this.timestampTo = timestampTo;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
