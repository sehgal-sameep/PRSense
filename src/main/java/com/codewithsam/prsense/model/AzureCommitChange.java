package com.codewithsam.prsense.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Represents a file change between two Azure DevOps commits
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AzureCommitChange {

    private String path;

    // "add", "edit", "delete", "rename" — from Azure DevOps diff API
    private String changeType;

    public boolean isDelete() {
        return "delete".equalsIgnoreCase(changeType);
    }

    public boolean isAddOrEdit() {
        return "add".equalsIgnoreCase(changeType)
                || "edit".equalsIgnoreCase(changeType)
                || "rename".equalsIgnoreCase(changeType);
    }
}
