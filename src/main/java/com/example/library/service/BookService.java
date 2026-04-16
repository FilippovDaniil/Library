package com.example.library.service;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import com.example.library.dto.BookDto;
import com.example.library.exception.LibraryException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис управления книгами.
 *
 * @Scope("singleton") — это поведение по умолчанию для всех Spring-бинов.
 * Один экземпляр на всё приложение. Указано явно для демонстрации.
 *
 * @Transactional на уровне класса задаёт дефолтное поведение для всех методов:
 * propagation = REQUIRED (присоединиться к существующей транзакции или создать новую),
 * isolation = DEFAULT (уровень изоляции БД по умолчанию — для PostgreSQL это READ COMMITTED).
 */
@Service
@Scope("singleton") // явно указан для демонстрации — по умолчанию и так singleton
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Добавление новой книги.
     * @Transactional гарантирует: если что-то пойдёт не так — изменения откатятся.
     */
    public Book addBook(String title, String author, String isbnValue, Integer year) {
        if (title == null || title.isBlank()) throw new LibraryException("Название книги не может быть пустым");
        if (author == null || author.isBlank()) throw new LibraryException("Автор не может быть пустым");

        ISBN isbn = new ISBN(isbnValue); // выбросит IllegalArgumentException при неверном формате

        if (bookRepository.findByIsbnValue(isbnValue).isPresent()) {
            throw new LibraryException("Книга с ISBN " + isbnValue + " уже существует");
        }

        Book book = new Book(title, author, isbn, year);
        return bookRepository.save(book);
    }

    /**
     * Удаление книги по ID.
     * Нельзя удалить книгу, которая сейчас выдана (isBorrowed = true).
     */
    public void deleteBook(Long id) {
        Book book = findById(id);
        if (book.isBorrowed()) {
            throw new LibraryException("Нельзя удалить книгу — она сейчас выдана читателю");
        }
        bookRepository.delete(book);
    }

    /**
     * Получение списка всех книг — readOnly = true.
     * Spring может оптимизировать: не сбрасывать flush, использовать read-only соединение.
     */
    @Transactional(readOnly = true)
    public List<BookDto> getAllBooks() {
        // Обычный findAll() без оптимизации — для простого списка без читателей
        return bookRepository.findAll().stream()
            .map(b -> new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), null))
            .toList();
    }

    /**
     * Оптимизированный запрос: книги + текущий читатель — ОДИН SQL с JOIN FETCH.
     * Решает проблему N+1: без JOIN FETCH Hibernate выполнял бы N SELECT для коллекций.
     */
    @Transactional(readOnly = true)
    public List<BookDto> getAllBooksWithCurrentReader() {
        return bookRepository.findAllWithActiveReadersFetch().stream()
            .map(b -> {
                String readerName = b.getBorrowRecords().stream()
                    .filter(br -> br.getReturnDate() == null)
                    .findFirst()
                    .map(br -> br.getReader().getFullName())
                    .orElse(null);
                return new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                    b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), readerName);
            })
            .toList();
    }

    /**
     * ДЕМОНСТРАЦИЯ N+1 ПРОБЛЕМЫ.
     * findAll() загружает книги без JOIN. Затем при обращении к borrowRecords
     * для каждой книги Hibernate делает ОТДЕЛЬНЫЙ SELECT → N+1 запросов в логах.
     *
     * В логах при show-sql=true будет видно множество запросов вида:
     *   SELECT * FROM borrow_records WHERE book_id = ?
     * — по одному на каждую книгу!
     */
    @Transactional(readOnly = true)
    public List<BookDto> getAllBooksWithReaderN1Demo() {
        List<Book> books = bookRepository.findAll(); // 1 SELECT
        return books.stream()
            .map(b -> {
                // Вот здесь — N дополнительных SELECT (по одному на каждую книгу)!
                String readerName = b.getBorrowRecords().stream()  // LAZY → SELECT
                    .filter(br -> br.getReturnDate() == null)
                    .findFirst()
                    .map(br -> br.getReader().getFullName())       // ещё N SELECT для reader
                    .orElse(null);
                return new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                    b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), readerName);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDto> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title).stream()
            .map(b -> new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDto> searchByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author).stream()
            .map(b -> new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new LibraryException("Книга с ID " + id + " не найдена"));
    }

    @Transactional(readOnly = true)
    public List<BookDto> getTop5MostBorrowed() {
        return bookRepository.findTop5MostBorrowed().stream()
            .map(b -> new BookDto(b.getId(), b.getTitle(), b.getAuthor(),
                b.getIsbn().getValue(), b.getPublicationYear(), b.isBorrowed(), null))
            .toList();
    }
}
