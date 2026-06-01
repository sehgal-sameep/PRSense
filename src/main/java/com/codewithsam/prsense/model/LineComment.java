package com.codewithsam.prsense.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineComment {
    private int line;
    private String comment;
    private String severity;
    private String suggestion; // optional short code snippet showing the fix
}
