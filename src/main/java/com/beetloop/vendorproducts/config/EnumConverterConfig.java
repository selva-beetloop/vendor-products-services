package com.beetloop.vendorproducts.config;

import com.beetloop.vendorproducts.domain.ProductCategory;
import com.beetloop.vendorproducts.domain.ProductStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC binds {@code @PathVariable} / {@code @RequestParam} enums with its
 * own converter, which only understands {@code Enum.valueOf} — it does not use
 * Jackson's {@code @JsonCreator}. Without these converters a perfectly valid
 * {@code ?category=raw-materials} would fail to bind, so the kebab-case ids the
 * frontend already uses have to be taught to the MVC layer explicitly.
 */
@Configuration
public class EnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new Converter<String, ProductCategory>() {
            @Override
            public ProductCategory convert(@NonNull String source) {
                return ProductCategory.from(source);
            }
        });
        registry.addConverter(new Converter<String, ProductStatus>() {
            @Override
            public ProductStatus convert(@NonNull String source) {
                return ProductStatus.from(source);
            }
        });
    }
}
