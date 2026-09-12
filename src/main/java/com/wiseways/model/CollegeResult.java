package com.wiseways.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One element inside the {@code "colleges"} array returned by POST /recommend.
 *
 * Mirrors the dict built in the Python {@code recommend()} route:
 * <pre>
 *   results.append({
 *       "college": ..., "branch": ..., "closing_rank": ...,
 *       "match_score": ..., "city": ..., "avg_package": ..., "fees": ...
 *   })
 * </pre>
 *
 * Snake-case JSON property names are preserved via {@code @JsonProperty}
 * so the existing frontend works without any changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeResult {

    private String college;
    private String branch;

    @JsonProperty("closing_rank")
    private double closingRank;

    @JsonProperty("match_score")
    private int matchScore;

    private String city;

    @JsonProperty("avg_package")
    private String avgPackage;

    private String fees;
}
