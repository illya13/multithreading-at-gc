package com.github.illya13.multithreading.lecture5;

import java.util.concurrent.CountDownLatch;

public class ThreadAllStates {
    private static final long TIMEOUT = 10000l;
    private static volatile boolean running = true;
    private static final Object lock = new Object();

    public static void main(String[] args) throws Exception {
        final CountDownLatch runnableLatch = new CountDownLatch(1);
        final CountDownLatch waitingLatch = new CountDownLatch(1);
        final CountDownLatch timedWaitingLatch = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            try {
                // RUNNABLE
                runnableLatch.countDown();
                while (running) {
                    // busy wait
                }

                // WAITING
                synchronized (lock) {
                    waitingLatch.countDown();
                    lock.wait();
                }

                // TIMED_WAITING
                synchronized (lock) {
                    timedWaitingLatch.countDown();
                    lock.wait(TIMEOUT);
                }
            } catch (InterruptedException e) {
                // ignore
            }
        });

        // 1. NEW
        System.out.println(thread.getState());

        thread.start();

        runnableLatch.await();
        // 2. RUNNABLE
        System.out.println(thread.getState());
        running = false;

        waitingLatch.await();
        synchronized (lock) {
            // 3. WAITING (can be in BLOCKED because of spurious waikups)
            System.out.println(thread.getState());
            lock.notify();

            // 4. BLOCKED
            System.out.println(thread.getState());
        }

        timedWaitingLatch.await();
        synchronized (lock) {
            // 5. TIMED_WAITING
            System.out.println(thread.getState());
            lock.notify();
        }

        thread.join();
        // 5. TERMINATED
        System.out.println(thread.getState());
    }
}
