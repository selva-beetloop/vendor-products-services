package com.beetloop.catalog.config;

import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data MongoDB stores BigDecimal as a String by default. Prices, assay percentages and
 * tier bounds all arrive as BigDecimal from the validation engine, and storing them as strings
 * breaks every derived figure that has to add them up.
 */
@Configuration
public class MongoDecimalConverters {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                BigDecimalToDecimal128.INSTANCE, Decimal128ToBigDecimal.INSTANCE));
    }

    @WritingConverter
    enum BigDecimalToDecimal128 implements Converter<BigDecimal, Decimal128> {
        INSTANCE;

        @Override
        public Decimal128 convert(BigDecimal source) {
            return new Decimal128(source);
        }
    }

    @ReadingConverter
    enum Decimal128ToBigDecimal implements Converter<Decimal128, BigDecimal> {
        INSTANCE;

        @Override
        public BigDecimal convert(Decimal128 source) {
            return source.bigDecimalValue();
        }
    }
}
