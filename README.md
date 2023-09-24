# countrysearch

## Purpose
It includes [`src/main/java/com/example/countrysearch/service/CountryService.java`](https://github.com/VonC/countrysearch/blob/main/src/main/java/com/example/countrysearch/service/CountryService.java), using an [`Aggregation`](https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/aggregation/Aggregation.html)

## Pipeline

The pipeline with the following steps:

1. **Filtered Cities Addition**: that filters cities based on whether their `from` field matches the `searchTerm`. The filtered array is then stored in a new field called `filteredCities`.

2. **Conditional Logic for Final Cities**: The pipeline checks whether `filteredCities` is empty (size is zero). If it is empty, a fallback occurs where the pipeline filters the cities based on the `detail` field set to `new_city`. The resulting array is stored in another new field called `finalCities`.

3. **Output Formatting**: The final operation in the pipeline projects (or selects) the fields that are required in the final output, such as `id`, `name`, and `finalCities`. The `finalCities` is renamed as `cities` in the final output.

```plaintext
   +----------------------+
   | Input Document       |
   +----------------------+
             |
             |
             v
 +------------------------+
 | Add 'filteredCities'    |  --->  Filter cities based on the 'from' field
 +------------------------+
             |
             |
             v
 +------------------------+
 | Conditional Logic      |  --->  Check if 'filteredCities' is empty; fallback to 'detail="new_city"'
 +------------------------+
             |
             |
             v
 +------------------------+
 | Output Formatting      |  --->  Project required fields ('id', 'name', 'finalCities')
 +------------------------+
             |
             |
             v
 +------------------------+
 | Final Output           |
 +------------------------+
```

## Code

### Full `CountryService.java` Code

```java
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

        // AggregationExpression for 'filteredCities'
        AggregationExpression filterCitiesExpression = ArrayOperators.Filter.filter("cities")
                .as("city")
                .by(ComparisonOperators.Eq.valueOf("city.from").equalToValue(searchTerm));

        // First addFields operation to add 'filteredCities'
        AggregationOperation addFieldsFilteredCities = project("id", "name", "cities")
                .and(filterCitiesExpression)
                .as("filteredCities");

        // AggregationExpression for conditional logic for 'finalCities'
        AggregationExpression conditionalFinalCities = ConditionalOperators.when(
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
```
