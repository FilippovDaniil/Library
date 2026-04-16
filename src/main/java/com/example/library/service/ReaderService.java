package com.example.library.service;

import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.ReaderRepository;
import com.example.library.dto.ReaderDto;
import com.example.library.exception.LibraryException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class ReaderService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ReaderRepository readerRepository;

    public ReaderService(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    public Reader addReader(String fullName, String email) {
        if (fullName == null || fullName.isBlank())
            throw new LibraryException("Имя читателя не может быть пустым");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches())
            throw new LibraryException("Неверный формат email: " + email);
        if (readerRepository.existsByEmail(email))
            throw new LibraryException("Читатель с email " + email + " уже зарегистрирован");

        return readerRepository.save(new Reader(fullName, email));
    }

    public void deleteReader(Long id) {
        Reader reader = findById(id);
        long activeBorrows = readerRepository.countActiveBorrows(id);
        if (activeBorrows > 0) {
            throw new LibraryException(
                "Нельзя удалить читателя — у него есть " + activeBorrows + " невозвращённых книг");
        }
        readerRepository.delete(reader);
    }

    @Transactional(readOnly = true)
    public List<ReaderDto> getAllReaders() {
        return readerRepository.findAll().stream()
            .map(r -> new ReaderDto(
                r.getId(), r.getFullName(), r.getEmail(), r.getMembershipDate(),
                readerRepository.countActiveBorrows(r.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Reader findById(Long id) {
        return readerRepository.findById(id)
            .orElseThrow(() -> new LibraryException("Читатель с ID " + id + " не найден"));
    }
}
