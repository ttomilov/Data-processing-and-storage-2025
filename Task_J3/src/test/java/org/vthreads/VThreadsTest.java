package org.vthreads;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VThreadsTest {
    @Test
    void newVirtualThreadExecutor() {
        ExecutorService executor = VThreads.newVirtualThreadExecutor();
        assertNotNull(executor, "The executor should not be null.");
    }
}