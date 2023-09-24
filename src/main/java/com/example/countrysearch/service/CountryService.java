package com.example.countrysearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service; // Import for @Service
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import org.bson.Document;
import java.util.Arrays;

import com.example.countrysearch.model.Country;

import java.util.List;

@Service  // Annotation to mark this class as a Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String searchTerm) {

        // First addFields operation to add 'filteredCities'
        AggregationOperation addFieldsFilteredCities = project("id", "name", "cities")
                .and(ArrayOperators.Filter.filter("cities")
                        .as("city")
                        .by(Criteria.where("city.from").is(searchTerm)))
                .as("filteredCities");

        // Conditional logic for 'finalCities' using native MongoDB expression
        AggregationExpression conditionalFinalCities = ConditionalOperators.when(
                new AggregationExpression() {
                    @Override
                    public Document toDocument(AggregationOperationContext context) {
                        return new Document("$eq", Arrays.asList(new Document("$size", "$filteredCities"), 0));
                    }
                })
                .then(
                        ArrayOperators.Filter.filter("cities")
                                .as("city")
                                .by(Criteria.where("city.detail").is("new_city"))
                )
                .otherwise("$filteredCities");

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