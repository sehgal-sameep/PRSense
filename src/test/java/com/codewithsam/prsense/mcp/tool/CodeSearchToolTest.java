package com.codewithsam.prsense.mcp.tool;

import com.codewithsam.prsense.mcp.dto.SearchCodeRequest;
import com.codewithsam.prsense.mcp.exception.ToolExecutionException;
import com.codewithsam.prsense.mcp.response.CodeSearchResult;
import com.codewithsam.prsense.mcp.service.CodeSearchToolService;
import com.codewithsam.prsense.mcp.service.ToolMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeSearchToolTest {

    @Mock private CodeSearchToolService codeSearchToolService;
    @Mock private ToolMetricsService toolMetricsService;
    @InjectMocks private CodeSearchTool tool;

    @Test
    void searchCode_delegatesToServiceAndRecordsSuccess() {
        CodeSearchResult expected = CodeSearchResult.builder()
                .query("user logic").totalResults(2).matches(List.of()).build();

        when(codeSearchToolService.search(any())).thenReturn(expected);

        SearchCodeRequest request = SearchCodeRequest.builder()
                .query("user logic").repository("repo").build();

        CodeSearchResult result = tool.searchCode(request);

        assertThat(result.getTotalResults()).isEqualTo(2);
        verify(toolMetricsService).recordToolStart(anyString(), anyString());
        verify(toolMetricsService).recordToolSuccess(anyString(), anyString(), anyLong());
        verify(toolMetricsService, never()).recordToolFailure(anyString(), anyString(), anyString());
    }

    @Test
    void searchCode_recordsFailureOnException() {
        when(codeSearchToolService.search(any())).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> tool.searchCode(
                SearchCodeRequest.builder().query("q").repository("r").build()))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("DB down");

        verify(toolMetricsService).recordToolFailure(anyString(), anyString(), anyString());
        verify(toolMetricsService, never()).recordToolSuccess(anyString(), anyString(), anyLong());
    }
}
