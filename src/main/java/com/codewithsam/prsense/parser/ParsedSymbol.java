package com.codewithsam.prsense.parser;

import com.codewithsam.prsense.constants.SymbolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A single semantic unit extracted from a source file — one chunk per symbol
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedSymbol {

    private String filePath;
    private String packageName;
    private String className;
    private String methodName;     // null for class/interface/enum-level symbols
    private SymbolType symbolType;
    private String content;        // source text of this symbol (used for embedding)
}
