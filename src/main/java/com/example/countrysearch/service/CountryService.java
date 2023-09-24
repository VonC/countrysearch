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

        // First addFields operation to add 'filteredCities'
        AggregationOperation addFieldsFilteredCities = project("id", "name", "cities")
                .and(ArrayOperators.Filter.filter("cities")
                        .as("city")
                        .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm)))
                .as("filteredCities");

        // Complex AggregationExpression for conditional logic for 'finalCities'
        AggregationExpression conditionalFinalCities = ConditionalOperators.when(
            ComparisonOperators.Gt.valueOf(
                ArrayOperators.Size.lengthOfArray(
                    ArrayOperators.Filter.filter("filteredCities")
                            .as("city")
                            .by(
                                BooleanOperators.And.and(
                                    ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm),
                                    ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(1000000)
                                )
                            )
                )
            ).greaterThanValue(0)
        )
        .then(
            ArrayOperators.Filter.filter("cities")
            .as("city")
            .by(
                BooleanOperators.And.and(
                    ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm),
                    ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(1000000)
                )
            )
        )
        .otherwise(
            ConditionalOperators.when(
                ComparisonOperators.Eq.valueOf(
                    ArrayOperators.Size.lengthOfArray("filteredCities")
                ).equalToValue(0)
            )
            .then(
                ArrayOperators.Filter.filter("cities")
                .as("city")
                .by(
                    ComparisonOperators.Eq.valueOf("city.detail").equalToValue("new_city")
                )
            )
            .otherwise("$filteredCities")
        );

        // Second addFields operation to add 'finalCities'
        AggregationOperation addFieldsFinalCities = project("id", "name")
                .and(conditionalFinalCities).as("finalCities");

        // Final project operation to shape the output
        AggregationOperation projectFinal = project("id", "name")
                .and("finalCities").as("cities");

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                addFieldsFilteredCities,
                addFieldsFinalCities,
                projectFinal
        );

        // Execute the aggregation
        AggregationResults<Country> result = mongoTemplate.aggregate(aggregation, "country", Country.class);

        return result.getMappedResults();
    }
}
