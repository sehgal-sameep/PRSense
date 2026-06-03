package com.codewithsam.prsense.dto.response;

import com.codewithsam.prsense.constants.SymbolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunkDto {

    private UUID id;
    private String repository;
    private String filePath;
    private String packageName;
    private String className;
    private String methodName;
    private SymbolType symbolType;
    private String content;
}
