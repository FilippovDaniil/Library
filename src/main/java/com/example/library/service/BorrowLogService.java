package com.example.library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервис аудит-логирования операций выдачи/возврата.
 *
 * Зачем REQUIRES_NEW?
 * Этот метод всегда запускает НОВУЮ независимую транзакцию, приостанавливая
 * текущую (если она есть). Это нужно, чтобы запись в лог была зафиксирована
 * (commit) независимо от того, откатится ли внешняя транзакция.
 *
 * Пример: borrowBook() выполняет проверки → создаёт BorrowRecord → вызывает logBorrow().
 * Если после logBorrow() что-то упадёт и основная транзакция откатится,
 * лог уже будет сохранён — для аудита это важно.
 *
 * В данном учебном проекте лог выводится в консоль/SLF4J, но в реальном
 * приложении здесь был бы INSERT в таблицу audit_log.
 */
@Service
public class BorrowLogService {

    private static final Logger log = LoggerFactory.getLogger(BorrowLogService.class);

    /**
     * propagation = REQUIRES_NEW:
     * - Приостанавливает текущую транзакцию (если есть)
     * - Открывает новое соединение с БД
     * - Выполняет INSERT в лог
     * - Делает commit независимо от внешней транзакции
     * - Возобновляет внешнюю транзакцию
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBorrow(String bookTitle, String readerName, String operation) {
        // В реальном проекте — INSERT в таблицу audit_log
        log.info("[AUDIT][{}] {} | книга: \"{}\" | читатель: {}",
            operation, LocalDateTime.now(), bookTitle, readerName);
    }
}
