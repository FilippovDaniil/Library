package com.example.library.domain.model;

import com.example.library.domain.embeddable.ISBN;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность Book.
 *
 * Поле borrowed — дублирует состояние для быстрого запроса "доступна ли книга"
 * без JOIN на BorrowRecord. Обновляется в BorrowService при выдаче/возврате.
 *
 * Связь с BorrowRecord — @OneToMany LAZY (по умолчанию для коллекций).
 * Это означает: при загрузке Book список borrowRecords НЕ загружается из БД
 * автоматически — только при явном обращении внутри @Transactional контекста.
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    /**
     * @Embedded — поля объекта ISBN встраиваются в таблицу books.
     * В БД появится колонка isbn_value.
     */
    @Embedded
    private ISBN isbn;

    @Column(name = "publication_year")
    private Integer publicationYear;

    /**
     * Флаг выдачи. Выбран подход с явным флагом (а не вычислением через BorrowRecord),
     * чтобы избежать дополнительного JOIN при простом показе списка книг.
     * Минус: нужно поддерживать консистентность вручную в сервисе.
     */
    @Column(nullable = false)
    private boolean borrowed = false;

    /**
     * LAZY загрузка — список записей о выдачах НЕ подгружается вместе с книгой.
     * Нужен только для истории; при обычном отображении книги он не нужен.
     * Обращение вне транзакции вызовет LazyInitializationException!
     */
    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BorrowRecord> borrowRecords = new ArrayList<>();

    protected Book() {}

    public Book(String title, String author, ISBN isbn, Integer publicationYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public ISBN getIsbn() { return isbn; }
    public void setIsbn(ISBN isbn) { this.isbn = isbn; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public boolean isBorrowed() { return borrowed; }
    public void setBorrowed(boolean borrowed) { this.borrowed = borrowed; }
    public List<BorrowRecord> getBorrowRecords() { return borrowRecords; }
}
