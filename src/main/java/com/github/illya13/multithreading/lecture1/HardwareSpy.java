package com.github.illya13.multithreading.lecture1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Logger;

public class HardwareSpy {
    private static final Logger logger = Logger.getLogger(HardwareSpy.class.getName());

    private List<Integer> caches;
    private int cores;

    public HardwareSpy() {
        caches = new ArrayList<>();
        initCacheSize();
        initCPUCores();
    }

    public int cacheLineSize() {
        int[] intArray = new int[64 * 1024 * 1024];

        Callable<Long> loopBaseline = () -> {
            long time = System.currentTimeMillis();
            for (int i = 0; i < intArray.length; i++)
                intArray[i] *= 2;
            return System.currentTimeMillis() - time;
        };

        IntFunction<Long> loopBy = (int dI) -> {
            long time = System.currentTimeMillis();
            for (int i = 0; i < intArray.length; i += dI)
                intArray[i] *= 2;
            return System.currentTimeMillis() - time;
        };

        long baseLine;
        try {
            baseLine = loopBaseline.call();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        logger.finest("baseline: " + baseLine);


        long current;
        int i = 2;
        do {
            current = loopBy.apply(i);
            logger.finest("by " + i + " " + current);
            i *= 2;
        } while (!hasChanged(baseLine, current));

        return i / 4 * 4;
    }

    private static boolean hasChanged(long baseLine, long current) {
        return Math.abs(baseLine - current) >= baseLine / 4;
    }

    private void initCacheSize() {
        Function<int[], Long> loopOver = (int[] intArray) -> {
            long time = System.currentTimeMillis();
            int steps = 64 * 1024 * 1024; // Arbitrary number of steps
            int lengthMod = intArray.length - 1;
            for (int i = 0; i < steps; i++)
                intArray[(i * 16) & lengthMod]++; // (x & lengthMod) is equal to (x % arr.Length)
            return System.currentTimeMillis() - time;
        };

        long baseLine = loopOver.apply(new int[128]);
        for (int k=1024; k<32*1024*1024; k *= 2) {
            long current = loopOver.apply(new int[k]);
            logger.finest(k + ": " + current);
            if (hasChanged(baseLine, current)) {
                logger.fine("next cache size: " + k / 2 * 4);
                caches.add(k / 2 * 4);
                baseLine = current;
            }
        }
    }

    private void initCPUCores() {
        IntFunction<Long> loopByThreads = (int n) -> {
            CountDownLatch start = new CountDownLatch(n);

            Runnable r = () -> {
                start.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                long s = 0;
                logger.finest(Thread.currentThread().getName());
                for (int i = 0; i < 1000000000; i++)
                    s += i;

                // disable optimisation
                if (s == 0)
                    throw new IllegalStateException();
            };

            Thread[] threads = new Thread[n];
            for (int i=0; i<n; i++)
                threads[i] = new Thread(r);

            long time = System.currentTimeMillis();
            for (int i=0; i<n; i++)
                threads[i].start();

            try {
                for (int i = 0; i < n; i++)
                    threads[i].join();
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie.getMessage(), ie);
            }
            return System.currentTimeMillis() - time;
        };

        long baseLine = loopByThreads.apply(1);
        logger.finest(1 + ": " + baseLine);
        for (int k=1; k<32; k *= 2) {
            long current = loopByThreads.apply(k);
            logger.finest(k + ": " + current);
            if (hasChanged(baseLine, current)) {
                logger.finest("cpu cores: " + k / 2);
                cores = k / 2;
                return;
            }
        }
    }

    public int cacheL1Size() {
        if (caches.size() < 2)
            return -1;
        return caches.get(0);
    }

    public int cacheL2Size() {
        if (caches.size() < 3)
            return -1;
        return caches.get(1);
    }

    public int cacheL3Size() {
        if (caches.size() < 4)
            return -1;
        return caches.get(2);
    }

    public int coreCount() {
        return cores;
    }

/*
    public int isSMP() {...}
    public int isNUMA() {...}
*/

    public static void main(String[] args) {
        for(int i=0; i<5; i++) {
            System.out.println("#" + i + " run");

            HardwareSpy hardwareSpy = new HardwareSpy();
            System.out.println("\tCPU cache line size: " + hardwareSpy.cacheLineSize() + " Bt");

            int l1 = hardwareSpy.cacheL1Size();
            System.out.print("\tCPU L1 cache size: " + l1);
            if (l1 != -1)
                System.out.println(" Bt = " + l1 / 1024 + " kBt");
            else
                System.out.println();

            int l2 = hardwareSpy.cacheL2Size();
            System.out.print("\tCPU L2 cache size: " + l2);
            if (l2 != -1)
                System.out.println(" Bt = " + l2 / 1024 + " kBt");
            else
                System.out.println();

            int l3 = hardwareSpy.cacheL3Size();
            System.out.print("\tCPU L3 cache size: " + l3);
            if (l3 != -1)
                System.out.println(" Bt = " + l3 / 1024 + " kBt");
            else
                System.out.println();

            System.out.println("\tCPU cores count: " + hardwareSpy.coreCount());
        }
    }
}