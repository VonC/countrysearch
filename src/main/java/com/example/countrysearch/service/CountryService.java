package com.example.countrysearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

import com.example.countrysearch.model.Country;

import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String searchTerm) {

        // Expression for 'filteredCities'
        AggregationOperation filterCities = project("id", "name", "cities")
                .and(ArrayOperators.Filter.filter("cities")
                        .as("city")
                        .by(BooleanOperators.And.and(
                            ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm),
                            ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(1000000)
                        )))
                .as("filteredCities");

        // Conditional logic for 'finalCities' a > b > c
        AggregationExpression conditionalFinalCities = ConditionalOperators
                .when(ComparisonOperators.Eq.valueOf(ArrayOperators.Size.lengthOfArray("filteredCities")).equalToValue(0))
                .then(
                    ConditionalOperators.when(
                        ComparisonOperators.Eq.valueOf(ArrayOperators.Size.lengthOfArray(
                                ArrayOperators.Filter.filter("cities")
                                    .as("city")
                                    .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm))
                        )).equalToValue(0)
                    )
                    .then(
                        ArrayOperators.Filter.filter("cities")
                            .as("city")
                            .by(ComparisonOperators.Eq.valueOf("city.detail").equalToValue("new_city"))
                    )
                    .otherwise(
                        ArrayOperators.Filter.filter("cities")
                            .as("city")
                            .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm))
                    )
                )
                .otherwise("$filteredCities");

        // Project 'finalCities' 
        AggregationOperation projectFinal = project("id", "name")
                .and(conditionalFinalCities).as("finalCities");

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                filterCities,
                projectFinal
        );

        // Execute the aggregation
        AggregationResults<Country> result = mongoTemplate.aggregate(aggregation, "country", Country.class);

        return result.getMappedResults();
    }
}
