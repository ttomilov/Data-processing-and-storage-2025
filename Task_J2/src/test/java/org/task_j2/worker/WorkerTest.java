package org.task_j2.worker;

import org.junit.jupiter.api.Test;
import org.task_j2.linkedList.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class WorkerTest {

    @Test
    void testWorker_sortsTheList() throws InterruptedException {
        LinkedList list = new LinkedList();
        list.addFirst('a');
        list.addFirst('b');
        list.addFirst('c');

        Worker worker = new Worker(list, 0, 0);
        worker.start();
        Thread.sleep(200);
        worker.interrupt();

        StringBuilder sb = new StringBuilder();
        for (char c : list) {
            sb.append(c);
        }

        assertEquals("abc", sb.toString());
        assertTrue(list.getTotalSwaps() > 0);
    }

}