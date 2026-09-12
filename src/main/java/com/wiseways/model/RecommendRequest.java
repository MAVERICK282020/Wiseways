package com.wiseways.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deserialised body of POST /recommend.
 *
 * Mirrors the fields extracted via {@code data.get(...)} in the Python
 * {@code recommend()} route.
 */
@Data
@NoArgsConstructor
public class RecommendRequest {

    /** General rank (JEE / UPTAC). */
    private int rank;

    /**
     * Category-specific rank (OBC, SC, ST, …).
     * Empty string or null means no category rank was supplied.
     */
    private String categoryRank;

    /** Preferred branch, e.g. "Computer Science and Engineering". */
    private String branch;

    /** Preferred city / area filter. Empty string means no filter. */
    private String area;

    /**
     * Fee range filter.
     * Accepted values: "any" | "5-10" | "10-20" | "20-40" | "40+"
     */
    private String budget;

    /**
     * Counselling board filter.
     * Accepted values: "any" | board name (case-insensitive).
     */
    private String counselling;
}
