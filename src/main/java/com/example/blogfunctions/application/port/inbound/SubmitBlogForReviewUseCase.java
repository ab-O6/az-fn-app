package com.example.blogfunctions.application.port.inbound;

import com.example.blogfunctions.domain.model.BlogSubmission;

public interface SubmitBlogForReviewUseCase {
    void submit(BlogSubmission submission);
}
