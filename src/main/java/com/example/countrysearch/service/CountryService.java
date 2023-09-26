package com.example.countrysearch.service;

import com.example.countrysearch.model.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String searchTerm) {

        // Define the conditional expressions for each case
        ConditionalOperators.Cond aCondition = ConditionalOperators.when(
                Criteria.where("cities.from").is(searchTerm).and("cities.population").gt(1000000))
                .then("$$ROOT.cities")
                .otherwise(null);

        ConditionalOperators.Cond bCondition = ConditionalOperators.when(
                Criteria.where("cities.from").is(searchTerm))
                .then("$$ROOT.cities")
                .otherwise(null);

        ConditionalOperators.Cond cCondition = ConditionalOperators.when(
                Criteria.where("cities.detail").is("new_city"))
                .then("$$ROOT.cities")
                .otherwise(null);

        // Unwind the cities
        UnwindOperation unwind = Aggregation.unwind("cities");

        // Apply the conditional logic
        ProjectionOperation project = Aggregation.project()
                .and(aCondition).as("caseA")
                .and(bCondition).as("caseB")
                .and(cCondition).as("caseC")
                .and("name").as("name")
                .and("_id").as("_id");

        // Re-group by ID and name, prioritizing A over B over C
        GroupOperation group = Aggregation.group("_id")
                .first("name").as("name")
                .push(ConditionalOperators.when(Criteria.where("caseA").ne(null)).then("caseA").otherwise(
                        ConditionalOperators.when(Criteria.where("caseB").ne(null)).then("caseB").otherwise("caseC")
                )).as("finalCities");

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(
                unwind,
                project,
                group,
                Aggregation.project("name", "finalCities")
        );

        // Execute the aggregation
        AggregationResults<Country> results = mongoTemplate.aggregate(aggregation, Country.class, Country.class);

        return results.getMappedResults();
    }
}
