package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.mcp.dto.FindImplementationsRequest;
import com.codewithsam.prsense.mcp.dto.FindReferencesRequest;
import com.codewithsam.prsense.mcp.response.ImplementationResult;
import com.codewithsam.prsense.mcp.response.ReferenceResult;
import com.codewithsam.prsense.mcp.service.ReferenceSearchService;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceSearchServiceImpl implements ReferenceSearchService {

    private final CodeChunkRepository codeChunkRepository;

    @Override
    public ReferenceResult findReferences(FindReferencesRequest request) {
        log.info("Reference search executed — repository: '{}', symbol: '{}'",
                request.getRepository(), request.getSymbol());

        String symbol = request.getSymbol();
        // Match the symbol as a whole word to reduce false positives
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(symbol) + "\\b");

        List<ReferenceResult.Reference> refs = codeChunkRepository
                .findByRepository(request.getRepository())
                .stream()
                .filter(c -> c.getContent() != null && pattern.matcher(c.getContent()).find())
                .filter(c -> !symbol.equals(c.getClassName()) || c.getSymbolType() != SymbolType.CLASS)
                .map(c -> ReferenceResult.Reference.builder()
                        .filePath(c.getFilePath())
                        .className(c.getClassName())
                        .methodName(c.getMethodName())
                        .symbolType(c.getSymbolType() != null ? c.getSymbolType().name() : null)
                        .build())
                .distinct()
                .toList();

        log.info("Reference search completed — symbol: '{}', references: {}", symbol, refs.size());

        return ReferenceResult.builder()
                .symbol(symbol)
                .totalReferences(refs.size())
                .references(refs)
                .build();
    }

    @Override
    public ImplementationResult findImplementations(FindImplementationsRequest request) {
        log.info("Implementation search executed — repository: '{}', interface: '{}'",
                request.getRepository(), request.getInterfaceName());

        String iface = request.getInterfaceName();
        Pattern pattern = Pattern.compile("\\bimplements\\b[^{]*\\b" + Pattern.quote(iface) + "\\b");

        List<ImplementationResult.Implementation> impls = codeChunkRepository
                .findByRepository(request.getRepository())
                .stream()
                .filter(c -> c.getSymbolType() == SymbolType.CLASS && c.getContent() != null)
                .filter(c -> pattern.matcher(c.getContent()).find())
                .map(c -> ImplementationResult.Implementation.builder()
                        .className(c.getClassName())
                        .filePath(c.getFilePath())
                        .packageName(c.getPackageName())
                        .build())
                .distinct()
                .toList();

        log.info("Implementation search completed — interface: '{}', implementations: {}", iface, impls.size());

        return ImplementationResult.builder()
                .interfaceName(iface)
                .totalImplementations(impls.size())
                .implementations(impls)
                .build();
    }
}
