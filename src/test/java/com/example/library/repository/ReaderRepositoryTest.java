package com.example.library.repository;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.BorrowRecord;
import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.BookRepository;
import com.example.library.domain.repository.BorrowRecordRepository;
import com.example.library.domain.repository.ReaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReaderRepositoryTest {

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private TestEntityManager em;

    private Reader ivan, maria;
    private Book book1, book2;

    @BeforeEach
    void setUp() {
        ivan  = readerRepository.save(new Reader("Иван Петров",    "ivan@test.com"));
        maria = readerRepository.save(new Reader("Мария Сидорова", "maria@test.com"));

        book1 = bookRepository.save(new Book("Книга 1", "Автор", new ISBN("978-5-04-001"), 2020));
        book2 = bookRepository.save(new Book("Книга 2", "Автор", new ISBN("978-5-04-002"), 2021));
    }

    // ===== existsByEmail =====

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertThat(readerRepository.existsByEmail("ivan@test.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(readerRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    // ===== countActiveBorrows =====

    @Test
    void countActiveBorrows_noActiveBorrows_returnsZero() {
        assertThat(readerRepository.countActiveBorrows(ivan.getId())).isZero();
    }

    @Test
    void countActiveBorrows_oneActiveBorrow_returnsOne() {
        borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        em.flush();

        assertThat(readerRepository.countActiveBorrows(ivan.getId())).isEqualTo(1L);
    }

    @Test
    void countActiveBorrows_returnedBorrowNotCounted() {
        BorrowRecord returned = new BorrowRecord(book1, ivan);
        returned.setReturnDate(LocalDate.now());
        borrowRecordRepository.save(returned);
        em.flush();

        // Возвращённая книга не считается активным долгом
        assertThat(readerRepository.countActiveBorrows(ivan.getId())).isZero();
    }

    @Test
    void countActiveBorrows_multipleActiveBooks_returnsCorrectCount() {
        borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        borrowRecordRepository.save(new BorrowRecord(book2, ivan));
        em.flush();

        assertThat(readerRepository.countActiveBorrows(ivan.getId())).isEqualTo(2L);
    }

    @Test
    void countActiveBorrows_otherReaderBorrows_notIncluded() {
        borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        borrowRecordRepository.save(new BorrowRecord(book2, maria));
        em.flush();

        // У Ивана 1, у Марии 1 — не пересекаются
        assertThat(readerRepository.countActiveBorrows(ivan.getId())).isEqualTo(1L);
        assertThat(readerRepository.countActiveBorrows(maria.getId())).isEqualTo(1L);
    }

    // ===== findDebtors =====

    @Test
    void findDebtors_readerWithOverdueBorrow_isReturned() {
        BorrowRecord br = borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        em.flush();
        // Переводим дату выдачи на 20 дней назад — это просрочка
        borrowRecordRepository.updateBorrowDate(br.getId(), LocalDate.now().minusDays(20));
        em.flush();
        em.clear();

        // cutoffDate = сегодня - 14 дней; Иван взял 20 дней назад → должник
        LocalDate cutoff = LocalDate.now().minusDays(14);
        List<Reader> debtors = readerRepository.findDebtors(cutoff);

        assertThat(debtors).extracting(Reader::getId).contains(ivan.getId());
    }

    @Test
    void findDebtors_readerBorrowedWithinLimit_notReturned() {
        // Взял 10 дней назад — ещё не просрочка (лимит 14 дней)
        BorrowRecord br = borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        em.flush();
        borrowRecordRepository.updateBorrowDate(br.getId(), LocalDate.now().minusDays(10));
        em.flush();
        em.clear();

        LocalDate cutoff = LocalDate.now().minusDays(14);
        List<Reader> debtors = readerRepository.findDebtors(cutoff);

        assertThat(debtors).extracting(Reader::getId).doesNotContain(ivan.getId());
    }

    @Test
    void findDebtors_returnedBorrow_notConsideredDebt() {
        BorrowRecord br = borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        br.setReturnDate(LocalDate.now());
        em.flush();
        borrowRecordRepository.updateBorrowDate(br.getId(), LocalDate.now().minusDays(30));
        em.flush();
        em.clear();

        // Книга возвращена — не должник, даже если взял давно
        List<Reader> debtors = readerRepository.findDebtors(LocalDate.now().minusDays(14));

        assertThat(debtors).extracting(Reader::getId).doesNotContain(ivan.getId());
    }

    @Test
    void findDebtors_noOverdues_returnsEmptyList() {
        List<Reader> debtors = readerRepository.findDebtors(LocalDate.now().minusDays(14));

        assertThat(debtors).isEmpty();
    }

    @Test
    void findDebtors_readerWithMultipleOverdues_appearsOnce() {
        // Читатель взял 2 книги 20 дней назад — должен быть в списке один раз (DISTINCT)
        BorrowRecord br1 = borrowRecordRepository.save(new BorrowRecord(book1, ivan));
        BorrowRecord br2 = borrowRecordRepository.save(new BorrowRecord(book2, ivan));
        em.flush();
        borrowRecordRepository.updateBorrowDate(br1.getId(), LocalDate.now().minusDays(20));
        borrowRecordRepository.updateBorrowDate(br2.getId(), LocalDate.now().minusDays(20));
        em.flush();
        em.clear();

        List<Reader> debtors = readerRepository.findDebtors(LocalDate.now().minusDays(14));

        long ivanCount = debtors.stream().filter(r -> r.getId().equals(ivan.getId())).count();
        assertThat(ivanCount).isEqualTo(1L); // DISTINCT — только один раз
    }
}
