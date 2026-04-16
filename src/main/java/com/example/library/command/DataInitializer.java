package com.example.library.command;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.BorrowRecord;
import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.BookRepository;
import com.example.library.domain.repository.BorrowRecordRepository;
import com.example.library.domain.repository.ReaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Инициализация демо-данных при первом запуске.
 * @Order(1) — выполняется раньше LibraryConsoleRunner (@Order(2)).
 */
@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public DataInitializer(BookRepository bookRepository,
                           ReaderRepository readerRepository,
                           BorrowRecordRepository borrowRecordRepository) {
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bookRepository.count() > 0) {
            log.info("Демо-данные уже загружены, пропускаем инициализацию");
            return;
        }

        log.info("Загрузка демо-данных...");

        // Книги
        Book book1 = bookRepository.save(new Book("Мастер и Маргарита", "Михаил Булгаков", new ISBN("978-5-389-06256-5"), 1967));
        Book book2 = bookRepository.save(new Book("1984", "Джордж Оруэлл", new ISBN("978-5-17-080115-0"), 1949));
        Book book3 = bookRepository.save(new Book("Преступление и наказание", "Фёдор Достоевский", new ISBN("978-5-04-089672-3"), 1866));
        Book book4 = bookRepository.save(new Book("Война и мир", "Лев Толстой", new ISBN("978-5-389-07489-6"), 1869));

        // Читатели
        Reader reader1 = readerRepository.save(new Reader("Иван Петров", "ivan@example.com"));
        Reader reader2 = readerRepository.save(new Reader("Мария Сидорова", "maria@example.com"));

        // Активная выдача (книга 1 у читателя 1)
        book1.setBorrowed(true);
        BorrowRecord active = new BorrowRecord(book1, reader1);
        borrowRecordRepository.save(active);

        // Просроченная выдача (book2 выдана 20 дней назад — попадёт в должники)
        book2.setBorrowed(true);
        BorrowRecord overdue = new BorrowRecord(book2, reader2);
        // Устанавливаем дату выдачи 20 дней назад через рефлексию обходим protected конструктор
        // Используем отдельный способ — создаём и сохраняем, потом патчим через JPQL не нужно:
        borrowRecordRepository.save(overdue);
        // Обновим borrowDate напрямую через нативный запрос чтобы сделать запись просроченной
        borrowRecordRepository.flush();
        borrowRecordRepository.updateBorrowDate(overdue.getId(), LocalDate.now().minusDays(20));

        // Исторические выдачи (возвращённые)
        BorrowRecord returned = new BorrowRecord(book3, reader1);
        borrowRecordRepository.save(returned);
        borrowRecordRepository.flush();
        borrowRecordRepository.updateBorrowDate(returned.getId(), LocalDate.now().minusDays(30));
        returned.setReturnDate(LocalDate.now().minusDays(10));

        log.info("Демо-данные загружены: {} книг, {} читателей", bookRepository.count(), readerRepository.count());
    }
}
