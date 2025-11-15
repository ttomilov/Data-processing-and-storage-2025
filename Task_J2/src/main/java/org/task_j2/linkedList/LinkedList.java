package org.task_j2.linkedList;

import org.task_j2.node.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
        List<Character> snapshot = new ArrayList<>();

        synchronized (this) {
            Node current = head;
            while (current != null) {
                snapshot.add(current.getData());
                current = current.getNext();
            }
        }

        return snapshot.iterator();
    }
}
