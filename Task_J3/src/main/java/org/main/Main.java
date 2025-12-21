package org.main;

import org.spider.Spider;

public class Main {
    public static void main(String[] args) {
        Spider spider = new Spider(args[0], Integer.parseInt(args[1]));
        spider.start();
    }
}
