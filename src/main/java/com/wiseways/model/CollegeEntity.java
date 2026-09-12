package com.wiseways.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for one college-program cutoff row.
 * Persisted once by seeding from the CSV datasets, then queried by the
 * recommendation engine (nearest-rank fallback uses a JPQL query).
 */
@Entity
@Table(name = "colleges", indexes = @Index(name = "idx_closing_rank", columnList = "closing_rank"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollegeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String college;

    @Column(nullable = false, length = 255)
    private String branch;

    // "year" is a reserved keyword in H2 — mapped to cutoff_year
    @Column(name = "cutoff_year")
    private Integer year;

    private double openingRank;

    private double closingRank;

    /** Integer encoding of branch */
    private int branchCode;

    private String city;

    private String counsellingBoard;

    private Double feesLakhs;

    private Double avgPackage;
}
