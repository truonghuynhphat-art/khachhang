package com.bank.dto;

public class AlertReviewRequest {
    private String note; // optional, không bắt buộc dùng nếu bạn muốn đơn giản
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}