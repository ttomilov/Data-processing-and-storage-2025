package org.task_j2.linkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkedListTest {
    private LinkedList list;

    @BeforeEach
    void setUp() {
        list = new LinkedList();
    }

    @Test
    void testAddFirst_addsToEmptyList() {
        list.addFirst('a');
        assertNotNull(list.getHead());
        assertEquals('a', list.getHead().getData());
        assertNull(list.getHead().getNext());
        assertNull(list.getHead().getPrev());
    }

    @Test
    void testAddFirst_createsCorrectOrder() {
        list.addFirst('c');
        list.addFirst('b');
        list.addFirst('a');

        assertEquals('a', list.getHead().getData());
        assertEquals('b', list.getHead().getNext().getData());
        assertEquals('c', list.getHead().getNext().getNext().getData());
    }

    @Test
    void testIterator_iteratesInCorrectOrder() {
        list.addFirst('c');
        list.addFirst('b');
        list.addFirst('a');

        StringBuilder sb = new StringBuilder();
        for (char c : list) {
            sb.append(c);
        }
        assertEquals("abc", sb.toString());
    }

    @Test
    void testIterator_onEmptyList() {
        assertFalse(list.iterator().hasNext());
    }

    @Test
    void testSwapCounter_incrementsCorrectly() {
        assertEquals(0, list.getTotalSwaps());
        list.incrementSwaps();
        list.incrementSwaps();
        assertEquals(2, list.getTotalSwaps());
    }
}