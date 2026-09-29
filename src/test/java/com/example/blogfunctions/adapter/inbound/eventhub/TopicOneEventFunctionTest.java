package com.example.blogfunctions.adapter.inbound.eventhub;

import com.example.blogfunctions.application.port.inbound.ProcessTopicOneEventUseCase;
import com.example.blogfunctions.configuration.JacksonConfiguration;
import com.example.blogfunctions.domain.model.TopicOneEvent;
import com.microsoft.azure.functions.ExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicOneEventFunctionTest {
    private final ProcessTopicOneEventUseCase useCase = mock(ProcessTopicOneEventUseCase.class);
    private final TopicOneEventFunction function = new TopicOneEventFunction(useCase, new JacksonConfiguration().jsonMapper());
    private final ExecutionContext context = mock(ExecutionContext.class);

    @Test void delegatesValidEventsAndSkipsPoisonEvents() {
        function.run(List.of("{\"id\":\"one\",\"data\":123}", "broken", "[]", "null", "{\"id\":5}", "{}"), context);
        verify(useCase).process(new TopicOneEvent("one", "{\"id\":\"one\",\"data\":123}"));
        verify(useCase).process(new TopicOneEvent(null, "{}"));
        verifyNoMoreInteractions(useCase);
    }
    @Test void processingFailurePropagatesToHostForRetry() {
        doThrow(new IllegalStateException("transient failure")).when(useCase).process(any());
        assertThrows(IllegalStateException.class, () -> function.run(List.of("{}", "{}"), context));
        verify(useCase, times(1)).process(any());
    }
    @Test void emptyBatchDoesNothing() {
        function.run(List.of(), context);
        verifyNoInteractions(useCase);
    }
}
