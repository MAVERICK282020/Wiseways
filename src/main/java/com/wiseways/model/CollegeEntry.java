package com.wiseways.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents one processed row from the CSV dataset.
 *
 * Mirrors the columns of the pandas DataFrame {@code df} in machine.py:
 *   college, branch, opening_rank, closing_rank, branch_code
 *
 * The optional fields (city, counsellingBoard, feesLakhs, avgPackage) come
 * from the enriched combined DataFrame if those columns exist in the CSV.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeEntry {

    private String college;
    private String branch;
    private Integer year;
    private double openingRank;
    private double closingRank;

    /** Integer encoding of branch — equivalent to df['branch_code'] */
    private int branchCode;

    // ── Optional enriched columns ─────────────────────────────────────────────
    private String city;
    private String counsellingBoard;
    private Double feesLakhs;
    private Double avgPackage;
}
