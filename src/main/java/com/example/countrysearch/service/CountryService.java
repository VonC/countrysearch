package com.example.countrysearch.service;

import com.example.countrysearch.model.Country;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CountryService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Country> findCountriesByCity(String cityName) {

        ProjectionOperation initialProjection = Aggregation.project("name", "cities")
                .and(ArrayOperators.Filter.filter("cities").as("city").by(
                        BooleanOperators.And.and(
                                ComparisonOperators.Eq.valueOf("city.from").equalToValue(cityName),
                                ComparisonOperators.Gt.valueOf("city.population").greaterThanValue(10)
                        )
                )).as("tempCitiesA")
                .and(ArrayOperators.Filter.filter("cities").as("city").by(
                        BooleanOperators.And.and(
                                ComparisonOperators.Eq.valueOf("city.from").equalToValue(cityName),
                                ComparisonOperators.Eq.valueOf("city.population").equalToValue(null)
                        )
                )).as("tempCitiesB");

        AggregationExpression sizeOfA = ArrayOperators.Size.lengthOfArray("tempCitiesA");
        AggregationExpression sizeOfB = ArrayOperators.Size.lengthOfArray("tempCitiesB");

        ConditionalOperators.Cond finalCondition = ConditionalOperators
                .when(ComparisonOperators.Eq.valueOf(sizeOfA).equalToValue(1))
                .then("$tempCitiesA")
                .otherwise(
                        ConditionalOperators
                        .when(ComparisonOperators.Eq.valueOf(sizeOfB).equalToValue(1))
                        .then("$tempCitiesB")
                        .otherwise(ArrayOperators.Filter.filter("cities")
                                .as("city")
                                .by(ComparisonOperators.Eq.valueOf("city.detail").equalToValue("new_city")))
                );

        ProjectionOperation finalProjection = Aggregation.project("name")
                .and(finalCondition).as("cities");

        Aggregation aggregation = Aggregation.newAggregation(initialProjection, finalProjection);

        AggregationResults<Country> results = mongoTemplate.aggregate(aggregation, "country", Country.class);

        return results.getMappedResults();
    }
}
