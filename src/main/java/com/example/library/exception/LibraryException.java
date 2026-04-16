package com.example.library.exception;

/**
 * Базовое unchecked исключение для бизнес-ошибок библиотеки.
 * RuntimeException → Spring автоматически откатывает транзакцию при его выбросе.
 * (По умолчанию @Transactional делает rollback только для RuntimeException и Error.)
 */
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
