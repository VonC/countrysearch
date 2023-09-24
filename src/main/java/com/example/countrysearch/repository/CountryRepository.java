package com.example.countrysearch.repository;

import com.example.countrysearch.model.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CountryRepository extends MongoRepository<Country, String> {
}
