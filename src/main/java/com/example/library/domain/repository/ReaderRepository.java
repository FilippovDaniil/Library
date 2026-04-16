package com.example.library.domain.repository;

import com.example.library.domain.model.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {

    Optional<Reader> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * JPQL: найти должников — читателей с невозвращёнными книгами старше 14 дней.
     * :cutoffDate = today - 14 days.
     * Возвращает DISTINCT читателей (у одного может быть несколько просроченных книг).
     */
    @Query("""
        SELECT DISTINCT r FROM Reader r
        JOIN r.borrowRecords br
        WHERE br.returnDate IS NULL
          AND br.borrowDate <= :cutoffDate
        """)
    List<Reader> findDebtors(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Считает количество активных (невозвращённых) выдач у читателя.
     * Используется при удалении читателя — нельзя удалить, если есть долги.
     */
    @Query("""
        SELECT COUNT(br) FROM BorrowRecord br
        WHERE br.reader.id = :readerId AND br.returnDate IS NULL
        """)
    long countActiveBorrows(@Param("readerId") Long readerId);
}
