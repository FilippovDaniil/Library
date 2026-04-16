package com.example.library.service;

import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.ReaderRepository;
import com.example.library.dto.ReaderDto;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReaderServiceTest {

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private ReaderService readerService;

    private Reader reader;

    @BeforeEach
    void setUp() {
        reader = new Reader("Иван Петров", "ivan@example.com");
        ReflectionTestUtils.setField(reader, "id", 1L);
        ReflectionTestUtils.setField(reader, "membershipDate", LocalDate.of(2024, 1, 15));
    }

    // ===== addReader =====

    @Test
    void addReader_validInput_savesAndReturnsReader() {
        when(readerRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(readerRepository.save(any(Reader.class))).thenReturn(reader);

        Reader result = readerService.addReader("Иван Петров", "ivan@example.com");

        assertThat(result.getFullName()).isEqualTo("Иван Петров");
        assertThat(result.getEmail()).isEqualTo("ivan@example.com");
        verify(readerRepository).save(any(Reader.class));
    }

    @Test
    void addReader_blankName_throwsLibraryException() {
        assertThatThrownBy(() -> readerService.addReader("", "ivan@example.com"))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("Имя");

        verifyNoInteractions(readerRepository);
    }

    @Test
    void addReader_nullName_throwsLibraryException() {
        assertThatThrownBy(() -> readerService.addReader(null, "ivan@example.com"))
            .isInstanceOf(LibraryException.class);

        verifyNoInteractions(readerRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing@", "@domain.com", "no-at-sign"})
    void addReader_invalidEmail_throwsLibraryException(String badEmail) {
        assertThatThrownBy(() -> readerService.addReader("Иван", badEmail))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("email");

        verifyNoInteractions(readerRepository);
    }

    @Test
    void addReader_duplicateEmail_throwsLibraryException() {
        when(readerRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> readerService.addReader("Другой Иван", "ivan@example.com"))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("уже зарегистрирован");

        verify(readerRepository, never()).save(any());
    }

    // ===== deleteReader =====

    @Test
    void deleteReader_noActiveBorrows_deletesReader() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(readerRepository.countActiveBorrows(1L)).thenReturn(0L);

        readerService.deleteReader(1L);

        verify(readerRepository).delete(reader);
    }

    @Test
    void deleteReader_notFound_throwsLibraryException() {
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.deleteReader(99L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("не найден");

        verify(readerRepository, never()).delete(any());
    }

    @Test
    void deleteReader_hasActiveBorrows_throwsLibraryException() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(readerRepository.countActiveBorrows(1L)).thenReturn(2L);

        assertThatThrownBy(() -> readerService.deleteReader(1L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("невозвращённых")
            .hasMessageContaining("2");

        verify(readerRepository, never()).delete(any());
    }

    // ===== getAllReaders =====

    @Test
    void getAllReaders_returnsMappedDtoList() {
        Reader reader2 = new Reader("Мария Сидорова", "maria@example.com");
        ReflectionTestUtils.setField(reader2, "id", 2L);
        ReflectionTestUtils.setField(reader2, "membershipDate", LocalDate.now());

        when(readerRepository.findAll()).thenReturn(List.of(reader, reader2));
        when(readerRepository.countActiveBorrows(1L)).thenReturn(1L);
        when(readerRepository.countActiveBorrows(2L)).thenReturn(0L);

        List<ReaderDto> result = readerService.getAllReaders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).fullName()).isEqualTo("Иван Петров");
        assertThat(result.get(0).activeBorrows()).isEqualTo(1L);
        assertThat(result.get(1).activeBorrows()).isEqualTo(0L);
    }

    @Test
    void getAllReaders_emptyRepository_returnsEmptyList() {
        when(readerRepository.findAll()).thenReturn(List.of());

        assertThat(readerService.getAllReaders()).isEmpty();
    }

    // ===== findById =====

    @Test
    void findById_existingId_returnsReader() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        Reader result = readerService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("ivan@example.com");
    }

    @Test
    void findById_missingId_throwsLibraryException() {
        when(readerRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.findById(77L))
            .isInstanceOf(LibraryException.class)
            .hasMessageContaining("77");
    }
}
