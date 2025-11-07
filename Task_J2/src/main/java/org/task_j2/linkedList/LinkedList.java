package org.task_j2.linkedList;

import org.task_j2.node.Node;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class LinkedList implements Iterable<Character> {
    private Node head;
    private volatile long totalSwaps = 0;

    public LinkedList() {
        this.head = null;
    }
    public synchronized void addFirst(char data) {
        Node newNode = new Node(data, null, this.head);
        if (this.head != null) {
            this.head.setPrev(newNode);
        }
        this.head = newNode;
    }

    public synchronized Node getHead() {
        return head;
    }

    public synchronized void setHead(Node head) {
        this.head = head;
    }

    synchronized public void incrementSwaps() {
        totalSwaps++;
    }

    public long getTotalSwaps() {
        return totalSwaps;
    }

    @Override
    public Iterator<Character> iterator() {
        return new Iterator<>() {
            private Node current = getHead();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Character next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Character data = current.getData();
                current = current.getNext();
                return data;
            }
        };
    }
}