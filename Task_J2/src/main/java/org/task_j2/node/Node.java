package org.task_j2.node;

public class Node {
    private char data;
    private Node next;
    private Node prev;

    public Node(char data, Node prev, Node next) {
        this.data = data;
        this.prev = prev;
        this.next = next;
    }

    public char getData() {
        return data;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public Node getPrev() {
        return prev;
    }

    public void setPrev(Node prev) {
        this.prev = prev;
    }
}