package com.example.countrysearch.service;

import com.example.countrysearch.model.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String cityName) {

        // Unwind cities array
        UnwindOperation unwind = Aggregation.unwind("cities");

        // Perform conditional projection to determine cityPriority
        ProjectionOperation projection = Aggregation.project("name", "cities")
                .and(ConditionalOperators
                        .when(Criteria.where("cities.from").is(cityName)
                                .and("cities.population").gte(1000000))
                        .then(true)
                        .otherwise(ConditionalOperators
                                .when(Criteria.where("cities.from").is(cityName))
                                .then(true)
                                .otherwise(false)
                        )
                ).as("cityPriority");

        // Group by _id and prepare the final list of cities
        GroupOperation group = Aggregation.group("_id")
                .first("name").as("name")
                .push(Aggregation.ROOT).as("finalCities");

        // Create aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                unwind,
                projection,
                group
        );

        // Execute the aggregation
        AggregationResults<Country> results = mongoTemplate.aggregate(aggregation, "country", Country.class);
        return results.getMappedResults();
    }
}