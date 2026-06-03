package com.codewithsam.prsense.parser;

import java.util.List;

// Strategy interface for language-specific source code parsing.
// Implement this to add support for Kotlin, TypeScript, Go, etc.
public interface CodeParser {

    // Returns true if this parser handles the given file extension
    boolean supports(String filePath);

    // Parses source code into a flat list of semantic symbols
    List<ParsedSymbol> parse(String filePath, String source);
}
