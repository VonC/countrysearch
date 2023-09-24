package com.example.countrysearch.controller;

import com.example.countrysearch.model.Country;
import com.example.countrysearch.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
public class CountryController {

    @Autowired
    private CountryService countryService;

    @GetMapping("/search")
    public List<Country> searchCountry(@RequestParam String city) {
        return countryService.findCountriesByCity(city);
    }
}
