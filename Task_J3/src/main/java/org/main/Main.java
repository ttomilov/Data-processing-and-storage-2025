package org.main;

import org.spider.Spider;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Spider spider = new Spider("localhost", 8080);
        spider.start();
    }
}
