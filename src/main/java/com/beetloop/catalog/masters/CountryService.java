package com.beetloop.catalog.masters;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Backs "Search country..." and the multi-country pickers (Markets Sold, Countries Exported To). */
@Service
public class CountryService {

    private static final List<Map<String, String>> COUNTRIES = List.of(
            Map.of("code", "IN", "label", "India", "iso3", "IND", "dialCode", "+91"),
            Map.of("code", "US", "label", "United States", "iso3", "USA", "dialCode", "+1"),
            Map.of("code", "CN", "label", "China", "iso3", "CHN", "dialCode", "+86"),
            Map.of("code", "DE", "label", "Germany", "iso3", "DEU", "dialCode", "+49"),
            Map.of("code", "GB", "label", "United Kingdom", "iso3", "GBR", "dialCode", "+44"),
            Map.of("code", "AE", "label", "United Arab Emirates", "iso3", "ARE", "dialCode", "+971"),
            Map.of("code", "IT", "label", "Italy", "iso3", "ITA", "dialCode", "+39"),
            Map.of("code", "JP", "label", "Japan", "iso3", "JPN", "dialCode", "+81"),
            Map.of("code", "KR", "label", "South Korea", "iso3", "KOR", "dialCode", "+82"),
            Map.of("code", "CA", "label", "Canada", "iso3", "CAN", "dialCode", "+1"),
            Map.of("code", "AU", "label", "Australia", "iso3", "AUS", "dialCode", "+61"),
            Map.of("code", "ID", "label", "Indonesia", "iso3", "IDN", "dialCode", "+62"),
            Map.of("code", "VN", "label", "Vietnam", "iso3", "VNM", "dialCode", "+84"));

    public List<Map<String, String>> search(String query) {
        if (query == null || query.isBlank()) {
            return COUNTRIES;
        }
        String needle = query.toLowerCase();
        return COUNTRIES.stream()
                .filter(c -> c.get("label").toLowerCase().contains(needle)
                        || c.get("code").toLowerCase().equals(needle))
                .toList();
    }
}
