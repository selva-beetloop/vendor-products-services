package com.beetloop.vendorproducts.pm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Counter backing the BRD §12.1 ID convention
 * <code>{ENTITY PREFIX}-{YYYY}-{NNNN}</code> — sequential per entity per year,
 * system-generated and never user-editable.
 */
@Entity
@Table(name = "pm_id_sequence")
@IdClass(PmIdSequence.Key.class)
public class PmIdSequence {

    @Id
    @Column(name = "prefix", length = 12, nullable = false)
    private String prefix;

    @Id
    @Column(name = "year_part", nullable = false)
    private int year;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;

    protected PmIdSequence() {
    }

    public PmIdSequence(String prefix, int year) {
        this.prefix = prefix;
        this.year = year;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getYear() {
        return year;
    }

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long nextValue) {
        this.nextValue = nextValue;
    }

    /** Composite key: one counter per (prefix, year). */
    public static class Key implements Serializable {

        private String prefix;
        private int year;

        public Key() {
        }

        public Key(String prefix, int year) {
            this.prefix = prefix;
            this.year = year;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return year == key.year && Objects.equals(prefix, key.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, year);
        }
    }
}
