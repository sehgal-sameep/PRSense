package com.codewithsam.prsense.service;

import com.codewithsam.prsense.model.FileDiff;
import com.codewithsam.prsense.model.LineComment;
import com.codewithsam.prsense.model.PrDetails;

import java.util.List;

// Agentic review: sends the diff to the LLM with all MCP tools attached.
// The LLM autonomously decides which tools to call before generating its final review.
// Replaces the static prompt-only flow in OpenAiServiceImpl for review execution.
public interface AgentReviewService {

    List<LineComment> reviewFile(PrDetails prDetails, FileDiff fileDiff,
                                 String crossFileContext, String repository,
                                 List<String> allChangedFilePaths);
}
