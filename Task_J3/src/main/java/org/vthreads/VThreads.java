package org.vthreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VThreads {

    public static ExecutorService newVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
