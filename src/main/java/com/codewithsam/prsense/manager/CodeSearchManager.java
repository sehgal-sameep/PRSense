package com.codewithsam.prsense.manager;

import com.codewithsam.prsense.dto.response.CodeSearchResponse;

public interface CodeSearchManager {

    CodeSearchResponse search(String query, String repository, int limit);
}
