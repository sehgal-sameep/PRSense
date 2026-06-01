package com.codewithsam.prsense.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrDetails {

    private String title;
    private String description;
    private String author;
    private String sourceBranch;
    private String targetBranch;
    private String repoId;
}
