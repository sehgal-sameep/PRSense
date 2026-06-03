package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.dto.request.IndexRepositoryRequest;
import com.codewithsam.prsense.dto.response.IndexStatusResponse;
import com.codewithsam.prsense.manager.RepositoryIndexManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepositoryIndexController.class)
class RepositoryIndexControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean RepositoryIndexManager repositoryIndexManager;

    @Test
    void postIndexRepository_returns202() throws Exception {
        doNothing().when(repositoryIndexManager).triggerIndexing(any());

        IndexRepositoryRequest request = IndexRepositoryRequest.builder()
                .repositoryName("my-service")
                .repositoryPath("/repos/my-service")
                .build();

        mockMvc.perform(post("/index/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    void postIndexRepository_returns400ForMissingFields() throws Exception {
        mockMvc.perform(post("/index/repository")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatus_returnsIndexStats() throws Exception {
        IndexStatusResponse response = IndexStatusResponse.builder()
                .repository("my-service")
                .totalChunks(120L)
                .totalFiles(15L)
                .lastIndexedAt(LocalDateTime.now())
                .build();

        when(repositoryIndexManager.getStatus(eq("my-service"))).thenReturn(response);

        mockMvc.perform(get("/index/status").param("repositoryName", "my-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repository").value("my-service"))
                .andExpect(jsonPath("$.totalChunks").value(120))
                .andExpect(jsonPath("$.totalFiles").value(15));
    }
}
