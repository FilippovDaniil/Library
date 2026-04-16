package com.example.library.service;

import com.example.library.domain.embeddable.ISBN;
import com.example.library.domain.model.Book;
import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.ReaderRepository;
import com.example.library.dto.BookDto;
import com.example.library.dto.ReaderDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты ReportService.
 *
 * Замечание: @PostConstruct НЕ вызывается при @ExtendWith(MockitoExtension.class),
 * так как Spring-контекст не поднимается. Для проверки @PostConstruct нужен
 * интеграционный тест с @SpringBootTest или @ExtendWith(SpringExtension.class).
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private ReportService reportService;

    // ===== getTop5Books =====

    @Test
    void getTop5Books_delegatesToBookService() {
        BookDto dto = new BookDto(1L, "1984", "Оруэлл", "978-5-17-080115-0", 1949, false, null);
        when(bookService.getTop5MostBorrowed()).thenReturn(List.of(dto));

        List<BookDto> result = reportService.getTop5Books();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("1984");
        verify(bookService).getTop5MostBorrowed();
    }

    @Test
    void getTop5Books_noBooks_returnsEmptyList() {
        when(bookService.getTop5MostBorrowed()).thenReturn(List.of());

        assertThat(reportService.getTop5Books()).isEmpty();
    }

    // ===== getDebtors =====

    @Test
    void getDebtors_returnsReadersWithOverdueBorrows() {
        Reader debtor = new Reader("Должник Иванов", "debtor@example.com");
        ReflectionTestUtils.setField(debtor, "id", 5L);
        ReflectionTestUtils.setField(debtor, "membershipDate", LocalDate.now().minusYears(1));

        // cutoffDate = now - 14 days; репозиторий возвращает должников по этой дате
        when(readerRepository.findDebtors(any(LocalDate.class))).thenReturn(List.of(debtor));
        when(readerRepository.countActiveBorrows(5L)).thenReturn(1L);

        List<ReaderDto> debtors = reportService.getDebtors();

        assertThat(debtors).hasSize(1);
        assertThat(debtors.get(0).fullName()).isEqualTo("Должник Иванов");
        assertThat(debtors.get(0).activeBorrows()).isEqualTo(1L);
    }

    @Test
    void getDebtors_noDebtors_returnsEmptyList() {
        when(readerRepository.findDebtors(any(LocalDate.class))).thenReturn(List.of());

        assertThat(reportService.getDebtors()).isEmpty();
    }

    @Test
    void getDebtors_cutoffDateIs14DaysAgo() {
        when(readerRepository.findDebtors(any(LocalDate.class))).thenReturn(List.of());

        reportService.getDebtors();

        // Проверяем, что cutoffDate = today - 14 дней
        LocalDate expectedCutoff = LocalDate.now().minusDays(14);
        verify(readerRepository).findDebtors(expectedCutoff);
    }
}
