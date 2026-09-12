package com.wiseways.model;

import lombok.Data;

/** Request body for POST /ask */
@Data
public class AskRequest {
    private String query;
}
