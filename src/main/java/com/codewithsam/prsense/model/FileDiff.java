package com.codewithsam.prsense.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDiff {

    private String path;
    private String changeType;
    private String before;
    private String after;
    private int lineCount;
}
