package org.task_j2.worker;

import org.task_j2.linkedList.LinkedList;
import org.task_j2.node.Node;

public class Worker extends Thread {
    private final LinkedList list;
    private final long stepDelay;
    private final long passDelay;

    public Worker(LinkedList list, long stepDelay, long passDelay) {
        this.list = list;
        this.stepDelay = stepDelay;
        this.passDelay = passDelay;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                bubbleSortPass();
                Thread.sleep(passDelay);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    private void bubbleSortPass() throws InterruptedException {
        Node current = list.getHead();
        if (current == null) {
            return;
        }

        while (current != null && current.getNext() != null) {
            Node firstLock = current;
            Node secondLock = current.getNext();

            synchronized (firstLock) {
                synchronized (secondLock) {
                    if (firstLock.getNext() == secondLock && secondLock.getPrev() == firstLock) {
                        if (firstLock.getData() > secondLock.getData()) {
                            swapNodes(firstLock, secondLock);
                            list.incrementSwaps();
                            current = secondLock;
                        }
                    }
                }
            }
            Thread.sleep(stepDelay);
            current = current.getNext();
        }
    }

    private void swapNodes(Node node1, Node node2) {
        Node prev1 = node1.getPrev();
        Node next2 = node2.getNext();

        if (prev1 != null) {
            prev1.setNext(node2);
        }
        node2.setPrev(prev1);
        node2.setNext(node1);
        node1.setPrev(node2);
        node1.setNext(next2);
        if (next2 != null) {
            next2.setPrev(node1);
        }

        if (list.getHead() == node1) {
            list.setHead(node2);
        }
    }
}