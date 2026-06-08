package com.codewithsam.prsense.mcp.service;

import com.codewithsam.prsense.constants.SymbolType;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import com.codewithsam.prsense.mcp.dto.AnalyzeImpactRequest;
import com.codewithsam.prsense.mcp.dto.GetClassHierarchyRequest;
import com.codewithsam.prsense.mcp.graph.RepositoryGraphBuilder;
import com.codewithsam.prsense.mcp.response.ClassHierarchyResult;
import com.codewithsam.prsense.mcp.response.ImpactAnalysisResult;
import com.codewithsam.prsense.mcp.service.impl.KnowledgeGraphServiceImpl;
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
class KnowledgeGraphServiceImplTest {

    @Mock private CodeChunkRepository codeChunkRepository;
    @InjectMocks private KnowledgeGraphServiceImpl service;

    // RepositoryGraphBuilder is a real component (lightweight, no DB calls)
    private final RepositoryGraphBuilder graphBuilder = new RepositoryGraphBuilder();

    @Test
    void getClassHierarchy_detectsInheritanceFromContent() {
        // Setup: UserServiceImpl implements UserService
        CodeChunkEntity implClass = CodeChunkEntity.builder()
                .repository("repo").className("UserServiceImpl")
                .symbolType(SymbolType.CLASS).filePath("UserServiceImpl.java")
                .content("public class UserServiceImpl implements UserService { }")
                .build();
        CodeChunkEntity iface = CodeChunkEntity.builder()
                .repository("repo").className("UserService")
                .symbolType(SymbolType.INTERFACE).filePath("UserService.java")
                .content("public interface UserService { }")
                .build();

        when(codeChunkRepository.findByRepository("repo")).thenReturn(List.of(implClass, iface));

        // Use the real graph builder by injecting it manually (bypasses @InjectMocks limitation)
        KnowledgeGraphServiceImpl realService =
                new KnowledgeGraphServiceImpl(codeChunkRepository, graphBuilder);

        ClassHierarchyResult result = realService.getClassHierarchy(
                GetClassHierarchyRequest.builder().className("UserServiceImpl").repository("repo").build());

        assertThat(result.getImplementedInterfaces()).contains("UserService");
    }

    @Test
    void analyzeImpact_returnsAffectedComponents() {
        CodeChunkEntity dep = CodeChunkEntity.builder()
                .repository("repo").className("UserController")
                .symbolType(SymbolType.CLASS).filePath("UserController.java")
                .content("private UserService userService;")
                .build();
        CodeChunkEntity target = CodeChunkEntity.builder()
                .repository("repo").className("UserService")
                .symbolType(SymbolType.CLASS).filePath("UserService.java")
                .content("class UserService { }")
                .build();

        when(codeChunkRepository.findByRepository("repo")).thenReturn(List.of(dep, target));

        KnowledgeGraphServiceImpl realService =
                new KnowledgeGraphServiceImpl(codeChunkRepository, graphBuilder);

        ImpactAnalysisResult result = realService.analyzeImpact(
                AnalyzeImpactRequest.builder().className("UserService").repository("repo").build());

        assertThat(result.getClassName()).isEqualTo("UserService");
        // UserController depends on UserService → should appear in affected controllers
        assertThat(result.getAffectedControllers()).contains("UserController");
    }
}
