package org.task_j2.main;

import org.task_j2.linkedList.LinkedList;
import org.task_j2.worker.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final long STEP_DELAY_MS = 100;
    private static final long PASS_DELAY_MS = 1000;

    public static void main(String[] args) {
        LinkedList linkedList = new LinkedList();
        List<Worker> workers = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCtrl+C нажат. Завершаем работу...");
            workers.forEach(Thread::interrupt);
            StringBuilder sb = new StringBuilder();
            for (char c : linkedList) {
                sb.append(c);
            }
            System.out.println(sb);
            System.out.println(linkedList.getTotalSwaps());
        }));

        for (int i = 0; i < Integer.parseInt(args[0]); i++) {
            Worker worker = new Worker(linkedList, STEP_DELAY_MS, PASS_DELAY_MS);
            workers.add(worker);
            worker.start();
        }

        System.out.println("Введите строку. Пустая строка для вывода текущего состояния строки. Нажмите CTRL+C для выхода + вывода строки на момент выхода.");
        Scanner sc = new Scanner(System.in);
        while (true) {
            String line = sc.nextLine();

            if (line.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (char c : linkedList) {
                    sb.append(c);
                }
                System.out.println(sb);
                continue;
            }

            for (int i = 0; i < line.length(); i++) {
                linkedList.addFirst(line.charAt(i));
            }
        }
    }
}