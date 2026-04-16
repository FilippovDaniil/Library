package com.example.library.service;

import com.example.library.domain.model.Reader;
import com.example.library.domain.repository.ReaderRepository;
import com.example.library.dto.BookDto;
import com.example.library.dto.ReaderDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Сервис отчётов.
 *
 * Демонстрирует @PostConstruct — метод вызывается ОДИН РАЗ Spring'ом
 * сразу после создания бина и внедрения всех зависимостей (@Autowired),
 * но ДО того, как бин начнёт обслуживать запросы.
 *
 * Порядок жизненного цикла бина:
 * 1. Создание объекта (new ReportService(...))
 * 2. Внедрение зависимостей (через конструктор — уже выполнено)
 * 3. @PostConstruct — вызывается здесь
 * 4. Бин готов к использованию
 * 5. @PreDestroy — при остановке контекста (не реализован, для справки)
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final BookService bookService;
    private final ReaderRepository readerRepository;

    public ReportService(BookService bookService, ReaderRepository readerRepository) {
        this.bookService = bookService;
        this.readerRepository = readerRepository;
    }

    /**
     * @PostConstruct — вызывается автоматически при старте Spring-контекста.
     * Здесь удобно: инициализировать кэш, проверить конфигурацию, вывести лог.
     */
    @PostConstruct
    public void init() {
        log.info("=== ReportService создан Spring-контейнером (бин-синглтон) ===");
        log.info("=== Готов к формированию отчётов ===");
    }

    @Transactional(readOnly = true)
    public List<BookDto> getTop5Books() {
        return bookService.getTop5MostBorrowed();
    }

    @Transactional(readOnly = true)
    public List<ReaderDto> getDebtors() {
        LocalDate cutoffDate = LocalDate.now().minusDays(14);
        List<Reader> debtors = readerRepository.findDebtors(cutoffDate);
        return debtors.stream()
            .map(r -> new ReaderDto(
                r.getId(), r.getFullName(), r.getEmail(), r.getMembershipDate(),
                readerRepository.countActiveBorrows(r.getId())))
            .toList();
    }
}
