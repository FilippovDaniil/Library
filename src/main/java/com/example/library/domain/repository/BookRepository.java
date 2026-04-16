package com.example.library.domain.repository;

import com.example.library.domain.model.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    // --- Поиск по названию и автору (JPQL с динамическим поиском) ---

    /**
     * JPQL: поиск по части названия без учёта регистра.
     * lower() — функция JPQL, работает в PostgreSQL и H2.
     */
    @Query("SELECT b FROM Book b WHERE lower(b.title) LIKE lower(concat('%', :title, '%'))")
    List<Book> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * JPQL: поиск по части имени автора без учёта регистра.
     */
    @Query("SELECT b FROM Book b WHERE lower(b.author) LIKE lower(concat('%', :author, '%'))")
    List<Book> findByAuthorContainingIgnoreCase(@Param("author") String author);

    Optional<Book> findByIsbnValue(String isbnValue);

    // --- Решение проблемы N+1: два варианта ---

    /**
     * ВАРИАНТ 1 — JOIN FETCH в JPQL.
     * Загружает Book + BorrowRecord + Reader одним SQL-запросом с JOIN.
     * В логах будет ОДИН запрос вместо N+1.
     *
     * Когда использовать: когда нужны данные связанных сущностей для всего списка.
     */
    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN FETCH b.borrowRecords br
        LEFT JOIN FETCH br.reader
        WHERE br.returnDate IS NULL OR br IS NULL
        """)
    List<Book> findAllWithActiveReadersFetch();

    /**
     * ВАРИАНТ 2 — @EntityGraph.
     * Аналог JOIN FETCH, но декларативный — Spring сам добавляет JOIN.
     * Разница от JOIN FETCH: не нужно писать JPQL вручную,
     * но сложнее контролировать точную форму запроса.
     *
     * Недостаток обоих вариантов: при FETCH коллекций возможно дублирование
     * строк (решается через DISTINCT или Set вместо List).
     */
    @EntityGraph(attributePaths = {"borrowRecords", "borrowRecords.reader"})
    @Query("SELECT b FROM Book b")
    List<Book> findAllWithEntityGraph();

    /**
     * Обычный findAll() БЕЗ оптимизации — для демонстрации N+1.
     * При обращении к book.getBorrowRecords() внутри транзакции
     * Hibernate выполнит отдельный SELECT для каждой книги.
     * В логах будет N+1 запросов (1 для списка книг + N для каждой коллекции).
     */
    // Используем унаследованный JpaRepository.findAll()

    // --- Топ-5 популярных книг ---

    /**
     * JPQL GROUP BY + ORDER BY COUNT.
     * Считает все записи выдачи (включая возвращённые) по каждой книге.
     */
    @Query("""
        SELECT b FROM Book b
        JOIN b.borrowRecords br
        GROUP BY b
        ORDER BY COUNT(br) DESC
        LIMIT 5
        """)
    List<Book> findTop5MostBorrowed();
}
