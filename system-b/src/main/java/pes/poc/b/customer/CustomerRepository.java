package pes.poc.b.customer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    long countBySourceTransferId(UUID sourceTransferId);

    List<CustomerEntity> findByCustomerIdIn(Collection<String> customerIds);
}
