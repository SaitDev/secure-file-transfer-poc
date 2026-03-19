package pes.poc.a.csv;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

@Service
public class MockCustomerCsvDataService {

    private static final List<String> FIRST_NAMES = List.of(
            "An",
            "Binh",
            "Chi",
            "Dung",
            "Giang",
            "Hanh",
            "Khanh",
            "Linh",
            "Minh",
            "Phuong"
    );

    private static final List<String> MIDDLE_NAMES = List.of(
            "Ngoc",
            "Thanh",
            "Thu",
            "Quoc",
            "Bao",
            "Hoang",
            "Gia",
            "My"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Nguyen",
            "Tran",
            "Le",
            "Pham",
            "Hoang",
            "Vo",
            "Dang",
            "Bui"
    );

    private static final List<String> STREETS = List.of(
            "Nguyen Hue",
            "Le Loi",
            "Hai Ba Trung",
            "Vo Van Tan",
            "Pasteur",
            "Pham Ngu Lao"
    );

    private static final List<String> DISTRICTS = List.of(
            "District 1",
            "District 3",
            "Binh Thanh",
            "Phu Nhuan",
            "Tan Binh",
            "Thu Duc"
    );

    private static final List<String> GENDERS = List.of("M", "F", "U");
    private static final int DEFAULT_RECORD_COUNT = 100;

    public String generateCsv() {
        return generateCsv(DEFAULT_RECORD_COUNT);
    }

    public String generateCsv(int recordCount) {
        return IntStream.range(0, recordCount)
                .mapToObj(index -> toCsvRow(createRecord()))
                .collect(Collectors.joining(System.lineSeparator(), header() + System.lineSeparator(), System.lineSeparator()));
    }

    private CustomerCsvRecord createRecord() {
        String firstName = pick(FIRST_NAMES);
        String middleName = pick(MIDDLE_NAMES);
        String lastName = pick(LAST_NAMES);
        String fullName = "%s %s %s".formatted(lastName, middleName, firstName);
        String customerId = "CUST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String email = "%s.%s.%s@example.test".formatted(
                lastName.toLowerCase(),
                middleName.toLowerCase(),
                firstName.toLowerCase()
        );

        return new CustomerCsvRecord(
                customerId,
                fullName,
                randomDateOfBirth(),
                pick(GENDERS),
                randomDigits(12),
                "0" + randomDigits(9),
                email,
                "%d %s, %s, Ho Chi Minh City".formatted(
                        ThreadLocalRandom.current().nextInt(1, 300),
                        pick(STREETS),
                        pick(DISTRICTS)
                )
        );
    }

    private String header() {
        return "customer_id,full_name,date_of_birth,gender,national_id,mobile_number,email,address_line";
    }

    private String toCsvRow(CustomerCsvRecord record) {
        return String.join(
                ",",
                escape(record.customerId()),
                escape(record.fullName()),
                escape(record.dateOfBirth().toString()),
                escape(record.gender()),
                escape(record.nationalId()),
                escape(record.mobileNumber()),
                escape(record.email()),
                escape(record.addressLine())
        );
    }

    private String escape(String value) {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String pick(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private LocalDate randomDateOfBirth() {
        return LocalDate.ofEpochDay(ThreadLocalRandom.current().nextLong(
                LocalDate.of(1970, 1, 1).toEpochDay(),
                LocalDate.of(2004, 12, 31).toEpochDay()
        ));
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }
}
