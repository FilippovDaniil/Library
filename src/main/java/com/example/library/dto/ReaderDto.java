package com.example.library.dto;

import java.time.LocalDate;

public record ReaderDto(
    Long id,
    String fullName,
    String email,
    LocalDate membershipDate,
    long activeBorrows
) {}
