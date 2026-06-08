package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.mcp.dto.FindImplementationsRequest;
import com.codewithsam.prsense.mcp.dto.FindReferencesRequest;
import com.codewithsam.prsense.mcp.response.ImplementationResult;
import com.codewithsam.prsense.mcp.response.ReferenceResult;
import com.codewithsam.prsense.mcp.service.impl.ReferenceSearchServiceImpl;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceSearchServiceImplTest {

    @Mock private CodeChunkRepository codeChunkRepository;
    @InjectMocks private ReferenceSearchServiceImpl service;

    @Test
    void findReferences_returnsChunksContainingSymbol() {
        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .repository("repo").className("OrderService").methodName("placeOrder")
                .symbolType(SymbolType.METHOD).filePath("OrderService.java")
                .content("UserService userService; userService.createUser(name);")
                .build();

        when(codeChunkRepository.findByRepository("repo")).thenReturn(List.of(chunk));

        ReferenceResult result = service.findReferences(
                FindReferencesRequest.builder().symbol("createUser").repository("repo").build());

        assertThat(result.getTotalReferences()).isEqualTo(1);
        assertThat(result.getReferences().get(0).getClassName()).isEqualTo("OrderService");
    }

    @Test
    void findReferences_returnsEmptyWhenSymbolNotFound() {
        CodeChunkEntity chunk = CodeChunkEntity.builder()
                .repository("repo").className("Foo").symbolType(SymbolType.CLASS)
                .filePath("Foo.java").content("class Foo {}").build();

        when(codeChunkRepository.findByRepository("repo")).thenReturn(List.of(chunk));

        ReferenceResult result = service.findReferences(
                FindReferencesRequest.builder().symbol("nonExistentMethod").repository("repo").build());

        assertThat(result.getTotalReferences()).isZero();
    }

    @Test
    void findImplementations_detectsImplementsClause() {
        CodeChunkEntity implClass = CodeChunkEntity.builder()
                .repository("repo").className("UserServiceImpl")
                .symbolType(SymbolType.CLASS).filePath("UserServiceImpl.java")
                .packageName("com.example")
                .content("public class UserServiceImpl implements UserService { }")
                .build();

        when(codeChunkRepository.findByRepository("repo")).thenReturn(List.of(implClass));

        ImplementationResult result = service.findImplementations(
                FindImplementationsRequest.builder().interfaceName("UserService").repository("repo").build());

        assertThat(result.getTotalImplementations()).isEqualTo(1);
        assertThat(result.getImplementations().get(0).getClassName()).isEqualTo("UserServiceImpl");
    }
}
