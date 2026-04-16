package com.example.library.command;

import com.example.library.dto.BookDto;
import com.example.library.dto.BorrowRecordDto;
import com.example.library.dto.ReaderDto;
import com.example.library.exception.LibraryException;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.ReaderService;
import com.example.library.service.ReportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/**
 * Главный консольный интерфейс.
 * @Order(2) — запускается после DataInitializer.
 *
 * Все сервисы внедряются через конструктор (рекомендуемый способ в Spring).
 */
@Component
@Order(2)
public class LibraryConsoleRunner implements ApplicationRunner {

    private final BookService bookService;
    private final ReaderService readerService;
    private final BorrowService borrowService;
    private final ReportService reportService;

    public LibraryConsoleRunner(BookService bookService,
                                ReaderService readerService,
                                BorrowService borrowService,
                                ReportService reportService) {
        this.bookService = bookService;
        this.readerService = readerService;
        this.borrowService = borrowService;
        this.reportService = reportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Scanner scanner = new Scanner(System.in)) {
            printWelcome();
            String command;
            do {
                printMenu();
                System.out.print("Введите команду: ");
                command = scanner.nextLine().trim();
                handleCommand(command, scanner);
            } while (!command.equalsIgnoreCase("exit") && !command.equals("0"));
        }
        System.out.println("До свидания!");
    }

    private void handleCommand(String command, Scanner scanner) {
        System.out.println();
        try {
            switch (command) {
                case "1"  -> addBook(scanner);
                case "2"  -> deleteBook(scanner);
                case "3"  -> showAllBooks();
                case "4"  -> searchBookByTitle(scanner);
                case "5"  -> searchBookByAuthor(scanner);
                case "6"  -> addReader(scanner);
                case "7"  -> deleteReader(scanner);
                case "8"  -> showAllReaders();
                case "9"  -> borrowBook(scanner);
                case "10" -> returnBook(scanner);
                case "11" -> showActiveBorrows();
                case "12" -> showReaderHistory(scanner);
                case "13" -> showTop5Books();
                case "14" -> showDebtors();
                case "15" -> showBooksOptimized();
                case "16" -> showBooksN1Demo();
                case "0", "exit" -> {} // обрабатывается в цикле
                default   -> System.out.println("Неизвестная команда. Введите число от 0 до 16.");
            }
        } catch (LibraryException e) {
            System.out.println("  [ОШИБКА] " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [ОШИБКА ВВОДА] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  [НЕОЖИДАННАЯ ОШИБКА] " + e.getMessage());
        }
        System.out.println();
    }

    // ===== КНИГИ =====

    private void addBook(Scanner sc) {
        System.out.println("--- Добавление книги ---");
        System.out.print("Название: ");
        String title = sc.nextLine().trim();
        System.out.print("Автор: ");
        String author = sc.nextLine().trim();
        System.out.print("ISBN (например 978-5-04-089672-3): ");
        String isbn = sc.nextLine().trim();
        System.out.print("Год издания: ");
        int year = readInt(sc);
        sc.nextLine();
        var book = bookService.addBook(title, author, isbn, year);
        System.out.printf("  Книга добавлена: [%d] %s%n", book.getId(), book.getTitle());
    }

    private void deleteBook(Scanner sc) {
        System.out.print("ID книги для удаления: ");
        long id = readLong(sc);
        sc.nextLine();
        bookService.deleteBook(id);
        System.out.println("  Книга удалена.");
    }

    private void showAllBooks() {
        List<BookDto> books = bookService.getAllBooks();
        if (books.isEmpty()) { System.out.println("  Книг нет."); return; }
        System.out.println("--- Все книги ---");
        books.forEach(b -> System.out.printf("  [%d] %-35s | %-25s | %s | %d | %s%n",
            b.id(), b.title(), b.author(), b.isbn(), b.publicationYear(), b.status()));
    }

    private void searchBookByTitle(Scanner sc) {
        System.out.print("Часть названия: ");
        String q = sc.nextLine().trim();
        List<BookDto> result = bookService.searchByTitle(q);
        printBookList(result);
    }

    private void searchBookByAuthor(Scanner sc) {
        System.out.print("Часть имени автора: ");
        String q = sc.nextLine().trim();
        List<BookDto> result = bookService.searchByAuthor(q);
        printBookList(result);
    }

    // ===== ЧИТАТЕЛИ =====

    private void addReader(Scanner sc) {
        System.out.println("--- Добавление читателя ---");
        System.out.print("Полное имя: ");
        String name = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        var reader = readerService.addReader(name, email);
        System.out.printf("  Читатель добавлен: [%d] %s%n", reader.getId(), reader.getFullName());
    }

    private void deleteReader(Scanner sc) {
        System.out.print("ID читателя для удаления: ");
        long id = readLong(sc);
        sc.nextLine();
        readerService.deleteReader(id);
        System.out.println("  Читатель удалён.");
    }

    private void showAllReaders() {
        List<ReaderDto> readers = readerService.getAllReaders();
        if (readers.isEmpty()) { System.out.println("  Читателей нет."); return; }
        System.out.println("--- Все читатели ---");
        readers.forEach(r -> System.out.printf(
            "  [%d] %-25s | %-30s | Рег: %s | Книг на руках: %d%n",
            r.id(), r.fullName(), r.email(), r.membershipDate(), r.activeBorrows()));
    }

    // ===== ВЫДАЧА / ВОЗВРАТ =====

    private void borrowBook(Scanner sc) {
        System.out.print("ID книги: ");
        long bookId = readLong(sc);
        System.out.print("ID читателя: ");
        long readerId = readLong(sc);
        sc.nextLine();
        var record = borrowService.borrowBook(bookId, readerId);
        System.out.printf("  Выдана книга [%d] читателю [%d], дата: %s%n",
            bookId, readerId, record.getBorrowDate());
    }

    private void returnBook(Scanner sc) {
        System.out.print("ID книги для возврата: ");
        long bookId = readLong(sc);
        sc.nextLine();
        var record = borrowService.returnBook(bookId);
        System.out.printf("  Книга возвращена, дата возврата: %s%n", record.getReturnDate());
    }

    private void showActiveBorrows() {
        List<BorrowRecordDto> list = borrowService.getAllActiveBorrows();
        if (list.isEmpty()) { System.out.println("  Нет текущих выдач."); return; }
        System.out.println("--- Текущие выдачи ---");
        list.forEach(br -> System.out.printf("  [%d] %-35s → %-25s | выдана: %s%n",
            br.id(), br.bookTitle(), br.readerName(), br.borrowDate()));
    }

    private void showReaderHistory(Scanner sc) {
        System.out.print("ID читателя: ");
        long id = readLong(sc);
        sc.nextLine();
        List<BorrowRecordDto> history = borrowService.getReaderHistory(id);
        if (history.isEmpty()) { System.out.println("  Нет истории."); return; }
        System.out.println("--- История выдач ---");
        history.forEach(br -> System.out.printf("  %-35s | выдана: %s | %s%n",
            br.bookTitle(), br.borrowDate(), br.status()));
    }

    // ===== ОТЧЁТЫ =====

    private void showTop5Books() {
        List<BookDto> top = reportService.getTop5Books();
        if (top.isEmpty()) { System.out.println("  Нет данных."); return; }
        System.out.println("--- Топ-5 популярных книг ---");
        for (int i = 0; i < top.size(); i++) {
            BookDto b = top.get(i);
            System.out.printf("  %d. [%d] %s — %s%n", i + 1, b.id(), b.title(), b.author());
        }
    }

    private void showDebtors() {
        List<ReaderDto> debtors = reportService.getDebtors();
        if (debtors.isEmpty()) { System.out.println("  Должников нет."); return; }
        System.out.println("--- Должники (книги не возвращены более 14 дней) ---");
        debtors.forEach(r -> System.out.printf("  [%d] %-25s | %s | книг на руках: %d%n",
            r.id(), r.fullName(), r.email(), r.activeBorrows()));
    }

    private void showBooksOptimized() {
        System.out.println("--- Книги с читателями (JOIN FETCH — оптимизированный запрос) ---");
        System.out.println("  (смотри логи — будет ОДИН SQL-запрос с JOIN)");
        List<BookDto> books = bookService.getAllBooksWithCurrentReader();
        books.forEach(b -> System.out.printf("  [%d] %-35s | %s%n", b.id(), b.title(), b.status()));
    }

    private void showBooksN1Demo() {
        System.out.println("--- Книги с читателями (N+1 демо — смотри логи!) ---");
        System.out.println("  (будет 1 + N запросов к БД для N книг)");
        List<BookDto> books = bookService.getAllBooksWithReaderN1Demo();
        books.forEach(b -> System.out.printf("  [%d] %-35s | %s%n", b.id(), b.title(), b.status()));
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private void printBookList(List<BookDto> books) {
        if (books.isEmpty()) { System.out.println("  Ничего не найдено."); return; }
        books.forEach(b -> System.out.printf("  [%d] %-35s | %-25s | %s%n",
            b.id(), b.title(), b.author(), b.isbn()));
    }

    private int readInt(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print("  Введите целое число: ");
            sc.next();
        }
        return sc.nextInt();
    }

    private long readLong(Scanner sc) {
        while (!sc.hasNextLong()) {
            System.out.print("  Введите число: ");
            sc.next();
        }
        return sc.nextLong();
    }

    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       БИБЛИОТЕКА — Spring Boot       ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    private void printMenu() {
        System.out.println("─────────────────────────────────────────");
        System.out.println("  Книги:      1-Добавить  2-Удалить  3-Все  4-Поиск(назв)  5-Поиск(авт)");
        System.out.println("  Читатели:   6-Добавить  7-Удалить  8-Все");
        System.out.println("  Выдача:     9-Выдать    10-Вернуть  11-Активные  12-История(чит)");
        System.out.println("  Отчёты:     13-Топ5     14-Должники");
        System.out.println("  Демо JPA:   15-Оптим.запрос  16-N+1 демо");
        System.out.println("  0/exit — Выход");
        System.out.println("─────────────────────────────────────────");
    }
}
