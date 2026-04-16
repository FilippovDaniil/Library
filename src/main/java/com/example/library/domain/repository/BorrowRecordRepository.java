package com.example.library.domain.repository;

import com.example.library.domain.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    /**
     * Найти активную (невозвращённую) запись выдачи по книге.
     * Используется при: проверке перед выдачей, возврате книги.
     */
    @Query("""
        SELECT br FROM BorrowRecord br
        JOIN FETCH br.reader
        JOIN FETCH br.book
        WHERE br.book.id = :bookId AND br.returnDate IS NULL
        """)
    Optional<BorrowRecord> findActiveByBookId(@Param("bookId") Long bookId);

    /**
     * Все активные выдачи (текущие должники).
     * JOIN FETCH — загружаем book и reader одним запросом.
     */
    @Query("""
        SELECT br FROM BorrowRecord br
        JOIN FETCH br.book
        JOIN FETCH br.reader
        WHERE br.returnDate IS NULL
        """)
    List<BorrowRecord> findAllActive();

    /**
     * История выдач конкретного читателя (все, включая возвращённые).
     */
    @Query("""
        SELECT br FROM BorrowRecord br
        JOIN FETCH br.book
        WHERE br.reader.id = :readerId
        ORDER BY br.borrowDate DESC
        """)
    List<BorrowRecord> findByReaderIdOrderByBorrowDateDesc(@Param("readerId") Long readerId);

    /** Используется DataInitializer для создания просроченных демо-данных. */
    @Modifying
    @Query("UPDATE BorrowRecord br SET br.borrowDate = :date WHERE br.id = :id")
    void updateBorrowDate(@Param("id") Long id, @Param("date") LocalDate date);
}
