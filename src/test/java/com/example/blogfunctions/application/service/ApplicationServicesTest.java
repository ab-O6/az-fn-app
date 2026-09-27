package com.example.blogfunctions.application.service;

import com.example.blogfunctions.application.port.inbound.*;
import com.example.blogfunctions.domain.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationServicesTest {
    @Test void submissionPortDispatchesToInitialService() {
        SubmitBlogForReviewUseCase useCase = new SubmitBlogForReviewService();
        assertDoesNotThrow(() -> useCase.submit(new BlogSubmission("title", "body", "author", null)));
    }
    @Test void eventPortDispatchesToInitialService() {
        ProcessTopicOneEventUseCase useCase = new ProcessTopicOneEventService();
        assertDoesNotThrow(() -> useCase.process(new TopicOneEvent("event-1")));
    }
}
