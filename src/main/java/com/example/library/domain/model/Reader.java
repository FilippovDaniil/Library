package com.example.library.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность Reader (Читатель).
 *
 * membershipDate заполняется автоматически через @PrePersist.
 * Связь с BorrowRecord — LAZY, аналогично Book.
 */
@Entity
@Table(name = "readers")
public class Reader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "membership_date", nullable = false)
    private LocalDate membershipDate;

    /**
     * LAZY — история выдач читателя не нужна при каждом обращении к объекту.
     */
    @OneToMany(mappedBy = "reader", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BorrowRecord> borrowRecords = new ArrayList<>();

    protected Reader() {}

    public Reader(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    /**
     * @PrePersist — вызывается JPA перед первым сохранением сущности.
     * Автоматически устанавливает дату регистрации.
     */
    @PrePersist
    private void prePersist() {
        if (membershipDate == null) {
            membershipDate = LocalDate.now();
        }
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getMembershipDate() { return membershipDate; }
    public List<BorrowRecord> getBorrowRecords() { return borrowRecords; }
}
