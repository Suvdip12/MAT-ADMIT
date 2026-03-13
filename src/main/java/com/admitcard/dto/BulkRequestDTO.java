package com.admitcard.dto;

import java.util.List;
import java.util.Map;

public class BulkRequestDTO {
    private Long templateId;
    private String outputType; // PDF or WORD
    private List<Map<String, String>> studentDataList;

    public BulkRequestDTO() {}

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public List<Map<String, String>> getStudentDataList() { return studentDataList; }
    public void setStudentDataList(List<Map<String, String>> studentDataList) { this.studentDataList = studentDataList; }
}
