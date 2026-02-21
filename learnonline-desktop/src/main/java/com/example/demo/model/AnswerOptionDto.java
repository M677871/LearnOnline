package com.example.demo.model;

/**
 * Answer option exposed to quiz taker (without correctness).
 *
 * <p>Returned by quiz endpoints so the UI can render options without leaking
 * which one is correct.</p>
 */
public class AnswerOptionDto {
    private Long id;
    private String answerText;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}
