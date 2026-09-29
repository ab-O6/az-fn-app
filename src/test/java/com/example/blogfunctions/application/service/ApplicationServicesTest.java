package com.example.blogfunctions.application.service;

import com.example.blogfunctions.application.port.inbound.*;
import com.example.blogfunctions.application.port.outbound.PublishGenericEventPort;
import com.example.blogfunctions.domain.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationServicesTest {
    @Test void submissionPortDispatchesToInitialService() {
        SubmitBlogForReviewUseCase useCase = new SubmitBlogForReviewService();
        assertDoesNotThrow(() -> useCase.submit(new BlogSubmission("title", "body", "author", null)));
    }
    @Test void eventPortDispatchesToServiceAndPublishes() {
        PublishGenericEventPort publisher = mock(PublishGenericEventPort.class);
        ProcessTopicOneEventUseCase useCase = new ProcessTopicOneEventService(publisher);
        assertDoesNotThrow(() -> useCase.process(new TopicOneEvent("event-1", "{\"key\":\"val\"}")));
        verify(publisher).publish("event-1", "{\"key\":\"val\"}");
    }
    @Test void eventPortRejectsBlankPayload() {
        PublishGenericEventPort publisher = mock(PublishGenericEventPort.class);
        ProcessTopicOneEventUseCase useCase = new ProcessTopicOneEventService(publisher);
        assertThrows(IllegalArgumentException.class, () -> useCase.process(new TopicOneEvent("event-1", "")));
        verifyNoInteractions(publisher);
    }
}

