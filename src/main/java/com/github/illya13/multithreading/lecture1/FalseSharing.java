package com.github.illya13.multithreading.lecture1;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class FalseSharing {
    private static final Logger logger = Logger.getLogger(HardwareSpy.class.getName());

    private volatile long v0 = 1;
    private volatile long v1 = 1;
    private volatile long v2 = 1;
    private volatile long v3 = 1;
    private volatile long v4 = 1;
    private volatile long v5 = 1;
    private volatile long v6 = 1;
    private volatile long v7 = 1;
    private volatile long v8 = 1;

    private volatile CountDownLatch start;
    private volatile CountDownLatch stop;
    private volatile ExecutorService threadPool;

    private Callable<Long> do_v0 = () -> {
        v0 = 1;
        start.countDown();
        start.await();

        long time = System.currentTimeMillis();
        for (int i=0; i<100000000; i++)
            v0 *= v0 + 1;
        time = System.currentTimeMillis() - time;

        stop.countDown();
        return time;
    };

    private Callable<Long> do_v1 = () -> {
        v1 = 1;
        start.countDown();
        start.await();

        long time = System.currentTimeMillis();
        for (int i=0; i<100000000; i++)
            v1 *= v1 + 1;
        time = System.currentTimeMillis() - time;

        stop.countDown();
        return time;
    };

    private Callable<Long> do_v8 = () -> {
        v8 = 1;
        start.countDown();
        start.await();

        long time = System.currentTimeMillis();
        for (int i=0; i<100000000; i++)
            v8 *= v8 + 1;
        time = System.currentTimeMillis() - time;

        stop.countDown();
        return time;
    };

    private long interceptor(Callable<Long> t1, Callable<Long> t2) {
        start = new CountDownLatch(2);
        stop = new CountDownLatch(2);
        threadPool = Executors.newFixedThreadPool(2);

        Future<Long> f1 = threadPool.submit(t1);
        Future<Long> f2 = threadPool.submit(t2);

        try {
            stop.await();
            return (f1.get() + f2.get()) / 2;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } finally {
            threadPool.shutdown();
        }
    }

    public long do_v0_v1() {
        return interceptor(do_v0, do_v1);
    }

    public long do_v0_v8() {
        return interceptor(do_v0, do_v8);
    }

    public long sum() {
        // avoid optimisation
        return v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7 + v8;
    }

    public static void main(String[] args) {
        FalseSharing falseSharing = new FalseSharing();

        logger.info("v0, v1: " + falseSharing.do_v0_v1());
        logger.info("v0, v8: " + falseSharing.do_v0_v8());

        // avoid optimisation
        logger.finest("sum = " + falseSharing.sum());
    }
}
