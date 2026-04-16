package com.example.library.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object для ISBN.
 *
 * Иммутабельность: класс final, поля final, нет сеттеров.
 * @Embeddable означает, что поля этого класса встраиваются прямо
 * в таблицу книги (колонка isbn_value), а не в отдельную таблицу.
 */
@Embeddable
public final class ISBN {

    @Column(name = "isbn_value", nullable = false, unique = true)
    private final String value;

    /**
     * JPA требует конструктор без аргументов для @Embeddable.
     * protected — чтобы нельзя было создать пустой ISBN извне.
     */
    protected ISBN() {
        this.value = null;
    }

    public ISBN(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ISBN не может быть пустым");
        }
        // Простая проверка: только цифры и дефисы, минимум 10 символов
        if (!value.matches("[\\d\\-X]{10,17}")) {
            throw new IllegalArgumentException(
                "Неверный формат ISBN: " + value + ". Ожидается строка из цифр и дефисов (10-17 символов)");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISBN isbn)) return false;
        return Objects.equals(value, isbn.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
