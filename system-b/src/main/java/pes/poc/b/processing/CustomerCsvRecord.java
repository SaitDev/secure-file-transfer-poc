package pes.poc.b.processing;

import java.time.LocalDate;

public record CustomerCsvRecord(
        String customerId,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String nationalId,
        String mobileNumber,
        String email,
        String addressLine
) {
}
