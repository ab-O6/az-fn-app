package com.example.blogfunctions.application.service;

import com.example.blogfunctions.application.port.inbound.SubmitBlogForReviewUseCase;
import com.example.blogfunctions.domain.model.BlogSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SubmitBlogForReviewService implements SubmitBlogForReviewUseCase {
    private static final Logger log = LoggerFactory.getLogger(SubmitBlogForReviewService.class);

    @Override
    public void submit(BlogSubmission value) {
        // Scaffold boundary: replace with a durable workflow before promising persistence.
        log.info("Blog submission outcome=accepted");
    }
}
