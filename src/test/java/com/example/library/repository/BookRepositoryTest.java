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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты BookRepository.
 * @DataJpaTest поднимает только JPA-слой с H2 in-memory.
 * Каждый тест выполняется в транзакции и откатывается после завершения —
 * данные между тестами не пересекаются.
 */
@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private TestEntityManager em;

    private Book bulg, orwell, dostoevsky;
    private Reader reader;

    @BeforeEach
    void setUp() {
        bulg      = bookRepository.save(new Book("Мастер и Маргарита", "Михаил Булгаков", new ISBN("978-5-04-001"), 1967));
        orwell    = bookRepository.save(new Book("1984",               "Джордж Оруэлл",  new ISBN("978-5-04-002"), 1949));
        dostoevsky= bookRepository.save(new Book("Идиот",              "Фёдор Достоевский", new ISBN("978-5-04-003"), 1869));

        reader = readerRepository.save(new Reader("Читатель Тест", "test@test.com"));
    }

    // ===== findByTitleContainingIgnoreCase =====

    @Test
    void findByTitle_partialMatch_returnsBook() {
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("маргарита");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void findByTitle_caseInsensitive_returnsBook() {
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("МАСТЕР");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void findByTitle_noMatch_returnsEmptyList() {
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("Гарри Поттер");

        assertThat(result).isEmpty();
    }

    @Test
    void findByTitle_partialMatchMultiple_returnsAll() {
        // Оба содержат букву "и" — убедимся, что возвращается несколько
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("и");

        assertThat(result.size()).isGreaterThanOrEqualTo(2);
    }

    // ===== findByAuthorContainingIgnoreCase =====

    @Test
    void findByAuthor_partialMatch_returnsBook() {
        List<Book> result = bookRepository.findByAuthorContainingIgnoreCase("Оруэлл");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor()).isEqualTo("Джордж Оруэлл");
    }

    @Test
    void findByAuthor_caseInsensitive_returnsBook() {
        List<Book> result = bookRepository.findByAuthorContainingIgnoreCase("БУЛГАКОВ");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByAuthor_noMatch_returnsEmptyList() {
        assertThat(bookRepository.findByAuthorContainingIgnoreCase("Толстой")).isEmpty();
    }

    // ===== findByIsbnValue =====

    @Test
    void findByIsbn_existingIsbn_returnsBook() {
        Optional<Book> found = bookRepository.findByIsbnValue("978-5-04-001");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void findByIsbn_nonExistentIsbn_returnsEmpty() {
        Optional<Book> found = bookRepository.findByIsbnValue("000-0-00-000000-0");

        assertThat(found).isEmpty();
    }

    // ===== findTop5MostBorrowed =====

    @Test
    void findTop5MostBorrowed_orderedByBorrowCount() {
        // bulg — 3 выдачи, orwell — 1 выдача, dostoevsky — 0 выдач
        saveReturnedBorrow(bulg);
        saveReturnedBorrow(bulg);
        saveReturnedBorrow(bulg);
        saveReturnedBorrow(orwell);
        em.flush();
        em.clear();

        List<Book> top5 = bookRepository.findTop5MostBorrowed();

        assertThat(top5).isNotEmpty();
        assertThat(top5.get(0).getId()).isEqualTo(bulg.getId()); // bulg на первом месте
        assertThat(top5.get(1).getId()).isEqualTo(orwell.getId());
    }

    @Test
    void findTop5MostBorrowed_noBorrows_returnsEmptyList() {
        // Нет ни одной записи выдачи — запрос через JOIN вернёт пустой список
        assertThat(bookRepository.findTop5MostBorrowed()).isEmpty();
    }

    // ===== findAllWithActiveReadersFetch (JOIN FETCH, без N+1) =====

    @Test
    void findAllWithActiveReadersFetch_bookWithActiveBorrow_includesReader() {
        BorrowRecord active = new BorrowRecord(bulg, reader);
        borrowRecordRepository.save(active);
        bulg.setBorrowed(true);
        em.flush();
        em.clear();

        List<Book> result = bookRepository.findAllWithActiveReadersFetch();

        // Книга с активной выдачей присутствует
        assertThat(result).extracting(Book::getId).contains(bulg.getId());
        // borrowRecords уже загружены (FETCH JOIN — не LAZY)
        Book found = result.stream().filter(b -> b.getId().equals(bulg.getId())).findFirst().orElseThrow();
        assertThat(found.getBorrowRecords()).anyMatch(br -> br.getReturnDate() == null);
    }

    @Test
    void findAllWithActiveReadersFetch_bookWithNoBorrows_isIncluded() {
        // dostoevsky никогда не выдавался — LEFT JOIN вернёт его с br IS NULL
        em.flush();
        em.clear();

        List<Book> result = bookRepository.findAllWithActiveReadersFetch();

        assertThat(result).extracting(Book::getId).contains(dostoevsky.getId());
    }

    // ===== Вспомогательный метод =====

    private void saveReturnedBorrow(Book b) {
        BorrowRecord br = new BorrowRecord(b, reader);
        br.setReturnDate(java.time.LocalDate.now());
        borrowRecordRepository.save(br);
    }
}
