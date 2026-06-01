package com.codewithsam.prsense.service;

import com.codewithsam.prsense.model.FileDiff;
import com.codewithsam.prsense.model.LineComment;
import com.codewithsam.prsense.model.PrDetails;

import java.util.List;

// Handles all communication with the OpenAI API for AI-powered code review
public interface OpenAiService {

    // Sends the file diff and context to OpenAI and returns a list of line-level review comments
    List<LineComment> reviewFile(PrDetails prDetails, FileDiff fileDiff, String crossFileContext);
}
