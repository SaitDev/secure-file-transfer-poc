package pes.poc.b.processing;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class CustomerCsvParser {

    private static final List<String> EXPECTED_HEADER = List.of(
            "customer_id",
            "full_name",
            "date_of_birth",
            "gender",
            "national_id",
            "mobile_number",
            "email",
            "address_line"
    );

    public List<CustomerCsvRecord> parse(byte[] csvBytes) {
        List<List<String>> rows = tokenize(new String(csvBytes, StandardCharsets.UTF_8));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Inbound CSV is empty");
        }
        if (!EXPECTED_HEADER.equals(rows.getFirst())) {
            throw new IllegalStateException("Inbound CSV header does not match the expected customer schema");
        }

        List<CustomerCsvRecord> records = new ArrayList<>();
        Set<String> customerIds = new HashSet<>();
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.size() == 1 && row.getFirst().isBlank()) {
                continue;
            }
            if (row.size() != EXPECTED_HEADER.size()) {
                throw new IllegalStateException("CSV row %d does not contain %d columns".formatted(index + 1, EXPECTED_HEADER.size()));
            }

            CustomerCsvRecord record = toRecord(row, index + 1);
            if (!customerIds.add(record.customerId())) {
                throw new IllegalStateException("Duplicate customer_id found in CSV: " + record.customerId());
            }
            records.add(record);
        }
        return records;
    }

    private CustomerCsvRecord toRecord(List<String> row, int rowNumber) {
        String customerId = required(row.get(0), "customer_id", rowNumber);
        String fullName = required(row.get(1), "full_name", rowNumber);
        String gender = required(row.get(3), "gender", rowNumber);
        String nationalId = required(row.get(4), "national_id", rowNumber);
        String mobileNumber = required(row.get(5), "mobile_number", rowNumber);
        String email = required(row.get(6), "email", rowNumber);
        String addressLine = required(row.get(7), "address_line", rowNumber);

        if (!Set.of("M", "F", "U").contains(gender)) {
            throw new IllegalStateException("Row %d has unsupported gender value: %s".formatted(rowNumber, gender));
        }

        try {
            return new CustomerCsvRecord(
                    customerId,
                    fullName,
                    LocalDate.parse(required(row.get(2), "date_of_birth", rowNumber)),
                    gender,
                    nationalId,
                    mobileNumber,
                    email,
                    addressLine
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException("Row %d has an invalid date_of_birth value".formatted(rowNumber), exception);
        }
    }

    private String required(String value, String fieldName, int rowNumber) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Row %d is missing %s".formatted(rowNumber, fieldName));
        }
        return value;
    }

    private List<List<String>> tokenize(String csvText) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < csvText.length(); index++) {
            char current = csvText.charAt(index);

            if (inQuotes) {
                if (current == '"') {
                    if (index + 1 < csvText.length() && csvText.charAt(index + 1) == '"') {
                        currentField.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(current);
                }
                continue;
            }

            if (current == '"') {
                inQuotes = true;
                continue;
            }
            if (current == ',') {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                continue;
            }
            if (current == '\r') {
                continue;
            }
            if (current == '\n') {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                continue;
            }

            currentField.append(current);
        }

        if (inQuotes) {
            throw new IllegalStateException("Inbound CSV contains an unterminated quoted field");
        }

        if (!currentRow.isEmpty() || currentField.length() > 0) {
            currentRow.add(currentField.toString());
            rows.add(currentRow);
        }

        return rows;
    }
}
