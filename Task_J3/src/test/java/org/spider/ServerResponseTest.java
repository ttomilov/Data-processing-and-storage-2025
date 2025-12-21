package org.spider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerResponseTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ServerResponse serverResponse;

    @BeforeEach
    void setUp() {
        serverResponse = new ServerResponse("http://localhost:8080", httpClient);
    }

    @Test
    void getResponse_Success() throws Exception {
        // Arrange
        String jsonResponse = "{\"message\":\"Test Message\",\"successors\":[\"/link1\",\"/link2\"]}";
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        // Act
        serverResponse.getResponse();

        // Assert
        assertEquals("Test Message", serverResponse.getMessage());
        assertNotNull(serverResponse.getSuccessors());
        assertEquals(2, serverResponse.getSuccessors().size());
        assertEquals(List.of("/link1", "/link2"), serverResponse.getSuccessors());
    }
}