package com.example.library.dto;

import java.time.LocalDate;

public record BorrowRecordDto(
    Long id,
    String bookTitle,
    String readerName,
    LocalDate borrowDate,
    LocalDate returnDate
) {
    public String status() {
        return returnDate == null ? "Не возвращена" : "Возвращена " + returnDate;
    }
}
