package com.example.library.dto;

/**
 * DTO для отображения книги в консоли.
 * Содержит только те данные, которые нужны для вывода — без JPA-прокси.
 */
public record BookDto(
    Long id,
    String title,
    String author,
    String isbn,
    Integer publicationYear,
    boolean borrowed,
    String currentReader  // null если книга в библиотеке
) {
    public String status() {
        if (borrowed && currentReader != null) {
            return "На руках у: " + currentReader;
        } else if (borrowed) {
            return "На руках";
        }
        return "В библиотеке";
    }
}
