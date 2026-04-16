package com.example.library.service;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.repository.BookRepository;
import com.example.library.dto.BookDto;
import com.example.library.exception.LibraryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// MockitoExtension — чистый unit-тест без Spring-контекста.
// @Transactional на сервисе в unit-тестах НЕ применяется (нет Spring AOP).
// Тестируем только бизнес-логику, репозиторий мокируем.
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book("Мастер и Маргарита", "Булгаков", new ISBN("978-5-04-089672-3"), 1967);
        ReflectionTestUtils.setField(book, "id", 1L);
    }

    // ===== addBook =====

    @Test
    void addBook_validInput_savesAndReturnsBook() {
        when(bookRepository.findByIsbnValue("978-5-04-089672-3")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        Book result = bookService.addBook("Мастер и Маргарита", "Булгаков", "978-5-04-089672-3", 1967);

        assertThat(result.getTitle()).isEqualTo("Мастер и Маргарита");
        assertThat(result.getIsbn().getValue()).isEqualTo("978-5-04-089672-3");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBook_blankTitle_throwsLibraryException() {
        assertThatThrownBy(() -> bookService.addBook("  ", "Булгаков", "978-5-04-089672-3", 2024))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("Название");

        verifyNoInteractions(bookRepository);
    }

    @Test
    void addBook_nullTitle_throwsLibraryException() {
        assertThatThrownBy(() -> bookService.addBook(null, "Автор", "978-5-04-089672-3", 2024))
            .isInstanceOf(LibraryException.class);

        verifyNoInteractions(bookRepository);
    }

    @Test
    void addBook_blankAuthor_throwsLibraryException() {
        assertThatThrownBy(() -> bookService.addBook("Название", "", "978-5-04-089672-3", 2024))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("Автор");

        verifyNoInteractions(bookRepository);
    }

    @Test
    void addBook_duplicateIsbn_throwsLibraryException() {
        when(bookRepository.findByIsbnValue("978-5-04-089672-3")).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.addBook("Другое название", "Автор", "978-5-04-089672-3", 2024))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("уже существует");

        verify(bookRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "invalid!", "abc"})
    void addBook_invalidIsbnFormat_throwsIllegalArgumentException(String badIsbn) {
        assertThatThrownBy(() -> bookService.addBook("Название", "Автор", badIsbn, 2024))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(bookRepository);
    }

    // ===== deleteBook =====

    @Test
    void deleteBook_bookExists_notBorrowed_deletesBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.deleteBook(1L);

        verify(bookRepository).delete(book);
    }

    @Test
    void deleteBook_bookNotFound_throwsLibraryException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("не найдена");

        verify(bookRepository, never()).delete(any());
    }

    @Test
    void deleteBook_bookIsBorrowed_throwsLibraryException() {
        book.setBorrowed(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.deleteBook(1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("выдана");

        verify(bookRepository, never()).delete(any());
    }

    // ===== getAllBooks =====

    @Test
    void getAllBooks_returnsMappedDtoList() {
        Book book2 = new Book("1984", "Оруэлл", new ISBN("978-5-17-080115-0"), 1949);
        ReflectionTestUtils.setField(book2, "id", 2L);

        when(bookRepository.findAll()).thenReturn(List.of(book, book2));

        List<BookDto> result = bookService.getAllBooks();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BookDto::title)
            .containsExactly("Мастер и Маргарита", "1984");
    }

    @Test
    void getAllBooks_emptyRepository_returnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());

        assertThat(bookService.getAllBooks()).isEmpty();
    }

    // ===== searchByTitle / searchByAuthor =====

    @Test
    void searchByTitle_delegatesToRepository() {
        when(bookRepository.findByTitleContainingIgnoreCase("маргар")).thenReturn(List.of(book));

        List<BookDto> result = bookService.searchByTitle("маргар");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void searchByAuthor_noMatches_returnsEmptyList() {
        when(bookRepository.findByAuthorContainingIgnoreCase("Толстой")).thenReturn(List.of());

        assertThat(bookService.searchByAuthor("Толстой")).isEmpty();
    }

    // ===== findById =====

    @Test
    void findById_existingId_returnsBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void findById_missingId_throwsLibraryException() {
        when(bookRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(42L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("42");
    }

    // ===== getTop5MostBorrowed =====

    @Test
    void getTop5MostBorrowed_delegatesToRepository() {
        when(bookRepository.findTop5MostBorrowed()).thenReturn(List.of(book));

        List<BookDto> top5 = bookService.getTop5MostBorrowed();

        assertThat(top5).hasSize(1);
        assertThat(top5.get(0).title()).isEqualTo("Мастер и Маргарита");
    }
}
