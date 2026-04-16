package com.example.library.service;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.BorrowRecord;
import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.BorrowRecordRepository;
import com.example.library.dto.BorrowRecordDto;
import com.example.library.exception.LibraryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock private BorrowRecordRepository borrowRecordRepository;
    @Mock private BookService bookService;
    @Mock private ReaderService readerService;
    @Mock private BorrowLogService borrowLogService;

    @InjectMocks
    private BorrowService borrowService;

    private Book book;
    private Reader reader;

    @BeforeEach
    void setUp() {
        book = new Book("1984", "Оруэлл", new ISBN("978-5-17-080115-0"), 1949);
        ReflectionTestUtils.setField(book, "id", 1L);

        reader = new Reader("Иван Петров", "ivan@example.com");
        ReflectionTestUtils.setField(reader, "id", 1L);
        ReflectionTestUtils.setField(reader, "membershipDate", LocalDate.now());
    }

    // ===== borrowBook =====

    @Test
    void borrowBook_validRequest_createsRecord() {
        when(bookService.findById(1L)).thenReturn(book);
        when(readerService.findById(1L)).thenReturn(reader);
        when(borrowRecordRepository.findActiveByBookId(1L)).thenReturn(Optional.empty());
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        BorrowRecord result = borrowService.borrowBook(1L, 1L);

        assertThat(result.getBook()).isEqualTo(book);
        assertThat(result.getReader()).isEqualTo(reader);
        assertThat(result.getBorrowDate()).isEqualTo(LocalDate.now());
        assertThat(result.getReturnDate()).isNull();
        assertThat(book.isBorrowed()).isTrue();

        verify(borrowRecordRepository).save(any(BorrowRecord.class));
        // BorrowLogService должен быть вызван с REQUIRES_NEW транзакцией
        verify(borrowLogService).logBorrow(book.getTitle(), reader.getFullName(), "BORROW");
    }

    @Test
    void borrowBook_bookFlagIsBorrowed_throwsLibraryException() {
        book.setBorrowed(true);
        when(bookService.findById(1L)).thenReturn(book);
        when(readerService.findById(1L)).thenReturn(reader);

        assertThatThrownBy(() -> borrowService.borrowBook(1L, 1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("уже выдана");

        verify(borrowRecordRepository, never()).save(any());
        verifyNoInteractions(borrowLogService);
    }

    @Test
    void borrowBook_activeRecordExists_throwsLibraryException() {
        // Флаг не выставлен (рассинхрон), но активная запись есть
        BorrowRecord activeRecord = new BorrowRecord(book, reader);
        when(bookService.findById(1L)).thenReturn(book);
        when(readerService.findById(1L)).thenReturn(reader);
        when(borrowRecordRepository.findActiveByBookId(1L)).thenReturn(Optional.of(activeRecord));

        assertThatThrownBy(() -> borrowService.borrowBook(1L, 1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("уже выдана");

        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    void borrowBook_bookNotFound_throwsLibraryException() {
        when(bookService.findById(99L)).thenThrow(new LibraryException("Книга с ID 99 не найдена"));

        assertThatThrownBy(() -> borrowService.borrowBook(99L, 1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("99");
    }

    @Test
    void borrowBook_readerNotFound_throwsLibraryException() {
        when(bookService.findById(1L)).thenReturn(book);
        when(readerService.findById(99L)).thenThrow(new LibraryException("Читатель с ID 99 не найден"));

        assertThatThrownBy(() -> borrowService.borrowBook(1L, 99L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("99");
    }

    // ===== returnBook =====

    @Test
    void returnBook_activeBorrowExists_setsReturnDate() {
        book.setBorrowed(true);
        BorrowRecord activeRecord = new BorrowRecord(book, reader);
        when(bookService.findById(1L)).thenReturn(book);
        when(borrowRecordRepository.findActiveByBookId(1L)).thenReturn(Optional.of(activeRecord));

        BorrowRecord result = borrowService.returnBook(1L);

        assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(book.isBorrowed()).isFalse();
        verify(borrowLogService).logBorrow(book.getTitle(), reader.getFullName(), "RETURN");
    }

    @Test
    void returnBook_noActiveRecord_throwsLibraryException() {
        when(bookService.findById(1L)).thenReturn(book);
        when(borrowRecordRepository.findActiveByBookId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowService.returnBook(1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("не числится");

        verifyNoInteractions(borrowLogService);
    }

    @Test
    void returnBook_bookNotFound_throwsLibraryException() {
        when(bookService.findById(99L)).thenThrow(new LibraryException("Книга с ID 99 не найдена"));

        assertThatThrownBy(() -> borrowService.returnBook(99L))
            .isInstanceOf(LibraryException.class);
    }

    // ===== getAllActiveBorrows =====

    @Test
    void getAllActiveBorrows_returnsActiveDtos() {
        BorrowRecord record = new BorrowRecord(book, reader);
        ReflectionTestUtils.setField(record, "id", 10L);
        when(borrowRecordRepository.findAllActive()).thenReturn(List.of(record));

        List<BorrowRecordDto> result = borrowService.getAllActiveBorrows();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookTitle()).isEqualTo("1984");
        assertThat(result.get(0).readerName()).isEqualTo("Иван Петров");
        assertThat(result.get(0).returnDate()).isNull();
    }

    @Test
    void getAllActiveBorrows_noBorrows_returnsEmptyList() {
        when(borrowRecordRepository.findAllActive()).thenReturn(List.of());

        assertThat(borrowService.getAllActiveBorrows()).isEmpty();
    }

    // ===== getReaderHistory =====

    @Test
    void getReaderHistory_returnsAllRecordsForReader() {
        BorrowRecord returned = new BorrowRecord(book, reader);
        returned.setReturnDate(LocalDate.now().minusDays(5));
        ReflectionTestUtils.setField(returned, "id", 5L);

        when(borrowRecordRepository.findByReaderIdOrderByBorrowDateDesc(1L))
            .thenReturn(List.of(returned));

        List<BorrowRecordDto> history = borrowService.getReaderHistory(1L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).bookTitle()).isEqualTo("1984");
        assertThat(history.get(0).returnDate()).isNotNull();
    }

    @Test
    void getReaderHistory_readerNotFound_throwsLibraryException() {
        when(readerService.findById(99L)).thenThrow(new LibraryException("Читатель с ID 99 не найден"));

        assertThatThrownBy(() -> borrowService.getReaderHistory(99L))
            .isInstanceOf(LibraryException.class);
    }
}
