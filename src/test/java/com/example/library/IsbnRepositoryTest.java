package com.example.library;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Интеграционный тест репозитория с использованием H2 in-memory (только для тестов).
 * @DataJpaTest поднимает только JPA-слой: сущности, репозитории, H2.
 * Spring Boot и Web-слой НЕ поднимаются — тест быстрый.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class IsbnRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldSaveAndFindBookByIsbn() {
        ISBN isbn = new ISBN("978-5-04-089672-3");
        Book book = new Book("Тест книга", "Автор", isbn, 2024);
        bookRepository.save(book);

        Optional<Book> found = bookRepository.findByIsbnValue("978-5-04-089672-3");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Тест книга");
        assertThat(found.get().getIsbn().getValue()).isEqualTo("978-5-04-089672-3");
    }

    @Test
    void isbnShouldBeEmbeddedInBookTable() {
        ISBN isbn = new ISBN("978-5-17-080115-0");
        Book book = new Book("Другая книга", "Другой автор", isbn, 2023);
        Book saved = bookRepository.save(book);

        // ISBN встроен в таблицу books — объект ISBN загружается вместе с Book
        assertThat(saved.getIsbn()).isNotNull();
        assertThat(saved.getIsbn()).isEqualTo(new ISBN("978-5-17-080115-0"));
    }

    @Test
    void isbnShouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new ISBN(""));
        assertThrows(IllegalArgumentException.class, () -> new ISBN(null));
        assertThrows(IllegalArgumentException.class, () -> new ISBN("not-valid!"));
    }

    @Test
    void isbnImmutability() {
        // ISBN — final класс, нет сеттеров, поля final
        ISBN isbn1 = new ISBN("978-5-04-089672-3");
        ISBN isbn2 = new ISBN("978-5-04-089672-3");
        assertThat(isbn1).isEqualTo(isbn2);  // equals по value
        assertThat(isbn1.hashCode()).isEqualTo(isbn2.hashCode());
    }
}
