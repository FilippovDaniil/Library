package com.example.library.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Сущность BorrowRecord — запись о выдаче книги.
 *
 * returnDate = null означает, что книга ещё не возвращена (активная выдача).
 *
 * Оба FK (@ManyToOne) — LAZY, чтобы не подтягивать Book и Reader при каждом
 * обращении к записи выдачи. JOIN FETCH используется явно там, где нужно.
 */
@Entity
@Table(name = "borrow_records")
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @ManyToOne LAZY — Book не подгружается автоматически.
     * Нужно явно указывать JOIN FETCH в JPQL или работать внутри транзакции.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * @ManyToOne LAZY — Reader не подгружается автоматически.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    /**
     * null = книга ещё не возвращена.
     * Не null = дата возврата.
     */
    @Column(name = "return_date")
    private LocalDate returnDate;

    protected BorrowRecord() {}

    public BorrowRecord(Book book, Reader reader) {
        this.book = book;
        this.reader = reader;
        this.borrowDate = LocalDate.now();
        this.returnDate = null;
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public Reader getReader() { return reader; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public boolean isActive() {
        return returnDate == null;
    }
}
