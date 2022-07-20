package io.wkrzywiec.fooddelivery.domain.customer;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "customers", path = "customers")
interface CustomerRepository extends CrudRepository<Customer, String> {
}
