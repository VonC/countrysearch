package com.example.countrysearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import org.springframework.stereotype.Service;

import com.example.countrysearch.model.Country;

import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String searchTerm) {

        // Filtering for condition "a"
        AggregationOperation filterForA = project("id", "name", "cities")
                .and(ArrayOperators.Filter.filter("cities")
                        .as("city")
                        .by(BooleanOperators.And.and(
                                ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm),
                                ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(1000000)
                        )))
                .as("filteredCitiesForA");

        // Check whether any document strictly meets condition "a"
        AggregationExpression sizeOfA = ArrayOperators.Size.lengthOfArray("filteredCitiesForA");
        
        // Check whether any document strictly meets condition "b"
        AggregationExpression sizeOfB = ArrayOperators.Size.lengthOfArray(
            ArrayOperators.Filter.filter("cities")
                .as("city")
                .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm))
        );

        // Conditional Logic
        AggregationExpression finalCondition = ConditionalOperators
            .when(ComparisonOperators.Gt.valueOf(sizeOfA).greaterThanValue(0))
            .then("$filteredCitiesForA")
            .otherwise(
                ConditionalOperators.when(ComparisonOperators.Gt.valueOf(sizeOfB).greaterThanValue(0))
                .then(
                    ArrayOperators.Filter.filter("cities")
                    .as("city")
                    .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm))
                )
                .otherwise(
                    ArrayOperators.Filter.filter("cities")
                    .as("city")
                    .by(ComparisonOperators.Eq.valueOf("city.detail").equalToValue("new_city"))
                )
            );

        // Project 'finalCities' 
        AggregationOperation projectFinal = project("id", "name")
                .and(finalCondition).as("finalCities");

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                filterForA,
                projectFinal
        );

        // Execute the aggregation
        AggregationResults<Country> result = mongoTemplate.aggregate(aggregation, "country", Country.class);

        return result.getMappedResults();
    }
}
