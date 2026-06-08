package com.codewithsam.prsense.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represents a file or folder entry in an Azure DevOps repository
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AzureRepositoryItem {

    private String path;
    private boolean folder;
    private String commitId;
    private String gitObjectType; // "blob" or "tree"
}
