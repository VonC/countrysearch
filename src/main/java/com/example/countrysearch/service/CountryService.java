package com.example.countrysearch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import com.example.countrysearch.model.Country;
import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String searchTerm) {
        AggregationExpression conditionAExpr = ConditionalOperators.when(
                BooleanOperators.And.and(
                        ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm),
                        ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(1000000)
                )
        ).thenValueOf("$$KEEP").otherwise(null);

        AggregationExpression conditionBExpr = ConditionalOperators.when(
                ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm)
        ).thenValueOf("$$KEEP").otherwise(null);

        AggregationExpression conditionCExpr = ConditionalOperators.when(
                ComparisonOperators.Eq.valueOf("city.detail").equalToValue("new_city")
        ).thenValueOf("$$KEEP").otherwise(null);

        Aggregation aggregation = newAggregation(
                project("id", "name", "cities")
                        .and(ArrayOperators.Filter.filter("cities")
                                .as("city")
                                .by(conditionAExpr))
                        .as("conditionACities")
                        .and(ArrayOperators.Filter.filter("cities")
                                .as("city")
                                .by(conditionBExpr))
                        .as("conditionBCities")
                        .and(ArrayOperators.Filter.filter("cities")
                                .as("city")
                                .by(conditionCExpr))
                        .as("conditionCCities"),
                project("id", "name")
                        .and(
                                ConditionalOperators.when(
                                        ComparisonOperators.Eq.valueOf(
                                                ArrayOperators.Size.lengthOfArray("conditionACities")
                                        ).equalToValue(0)
                                ).thenValueOf(
                                        ConditionalOperators.when(
                                                ComparisonOperators.Eq.valueOf(
                                                        ArrayOperators.Size.lengthOfArray("conditionBCities")
                                                ).equalToValue(0)
                                        ).then("$conditionCCities").otherwise("$conditionBCities")
                                ).otherwise("$conditionACities")
                        ).as("finalCities")
        );

        AggregationResults<Country> result = mongoTemplate.aggregate(aggregation, "country", Country.class);

        return result.getMappedResults();
    }
}
