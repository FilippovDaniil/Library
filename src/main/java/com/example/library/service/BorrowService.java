package com.example.library.service;

import com.example.library.domain.model.Book;
import com.example.library.domain.model.BorrowRecord;
import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.BorrowRecordRepository;
import com.example.library.dto.BorrowRecordDto;
import com.example.library.exception.LibraryException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookService bookService;
    private final ReaderService readerService;
    private final BorrowLogService borrowLogService;

    public BorrowService(BorrowRecordRepository borrowRecordRepository,
                         BookService bookService,
                         ReaderService readerService,
                         BorrowLogService borrowLogService) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookService = bookService;
        this.readerService = readerService;
        this.borrowLogService = borrowLogService;
    }

    /**
     * Выдача книги читателю.
     *
     * isolation = REPEATABLE_READ — зачем это нужно:
     * В конкурентной среде (несколько потоков/соединений) без этого уровня
     * возможна гонка: два потока проверяют "книга свободна?" → оба видят true →
     * оба выдают одну книгу разным читателям (phantom read / non-repeatable read).
     * REPEATABLE_READ гарантирует: данные, прочитанные в начале транзакции,
     * не изменятся до её конца. Строка книги "блокируется" для повторного чтения.
     *
     * В консольном приложении с одним пользователем это не критично,
     * но для обучения — обязательно показать.
     *
     * propagation = REQUIRED (по умолчанию) — если уже есть активная транзакция,
     * присоединяемся к ней; если нет — создаём новую.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BorrowRecord borrowBook(Long bookId, Long readerId) {
        Book book = bookService.findById(bookId);
        Reader reader = readerService.findById(readerId);

        // Проверяем актуальное состояние в рамках REPEATABLE_READ транзакции
        if (book.isBorrowed()) {
            throw new LibraryException(
                "Книга \"" + book.getTitle() + "\" уже выдана другому читателю");
        }

        // Дополнительная проверка через BorrowRecord (защита от рассинхрона флага)
        borrowRecordRepository.findActiveByBookId(bookId).ifPresent(br -> {
            throw new LibraryException(
                "Книга уже выдана: " + br.getReader().getFullName());
        });

        book.setBorrowed(true);
        BorrowRecord record = new BorrowRecord(book, reader);
        BorrowRecord saved = borrowRecordRepository.save(record);

        // Логируем выдачу в отдельной транзакции (REQUIRES_NEW).
        // Смысл: даже если основная транзакция откатится — лог сохранится.
        // Пример: если после save(record) произойдёт ошибка, выдача откатится,
        // но запись в лог (borrowLogService) останется — для аудита.
        borrowLogService.logBorrow(book.getTitle(), reader.getFullName(), "BORROW");

        return saved;
    }

    /**
     * Возврат книги.
     *
     * propagation = REQUIRED — участвует в существующей транзакции или создаёт новую.
     * При ошибке (книга не найдена, не выдана) — RuntimeException → rollback.
     */
    @Transactional
    public BorrowRecord returnBook(Long bookId) {
        Book book = bookService.findById(bookId);

        BorrowRecord record = borrowRecordRepository.findActiveByBookId(bookId)
            .orElseThrow(() -> new LibraryException(
                "Книга \"" + book.getTitle() + "\" не числится как выданная"));

        record.setReturnDate(LocalDate.now());
        book.setBorrowed(false);

        // Логируем возврат в отдельной транзакции
        borrowLogService.logBorrow(book.getTitle(), record.getReader().getFullName(), "RETURN");

        return record;
    }

    @Transactional(readOnly = true)
    public List<BorrowRecordDto> getAllActiveBorrows() {
        return borrowRecordRepository.findAllActive().stream()
            .map(br -> new BorrowRecordDto(
                br.getId(), br.getBook().getTitle(), br.getReader().getFullName(),
                br.getBorrowDate(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowRecordDto> getReaderHistory(Long readerId) {
        readerService.findById(readerId); // проверяем существование
        return borrowRecordRepository.findByReaderIdOrderByBorrowDateDesc(readerId).stream()
            .map(br -> new BorrowRecordDto(
                br.getId(), br.getBook().getTitle(), null,
                br.getBorrowDate(), br.getReturnDate()))
            .toList();
    }
}
