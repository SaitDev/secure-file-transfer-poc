package pes.poc.b.customer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "customer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_customer_id",
                columnNames = "customer_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity {

    @Id
    private UUID id;

    @Column(name = "source_transfer_id", nullable = false)
    private UUID sourceTransferId;

    @Column(name = "customer_id", nullable = false, length = 32)
    private String customerId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Column(name = "national_id", nullable = false, length = 32)
    private String nationalId;

    @Column(name = "mobile_number", nullable = false, length = 32)
    private String mobileNumber;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
