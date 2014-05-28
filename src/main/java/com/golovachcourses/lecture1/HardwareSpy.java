package com.golovachcourses.lecture1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
            long time = System.nanoTime();
            for (int i = 0; i < intArray.length; i++)
                intArray[i] *= 2;
            return System.nanoTime() - time;
        };

        IntFunction<Long> loopBy = (int dI) -> {
            long time = System.nanoTime();
            for (int i = 0; i < intArray.length; i += dI)
                intArray[i] *= 2;
            return System.nanoTime() - time;
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
        } while (!isChanged(baseLine, current));

        return i / 4 * 4;
    }

    private static boolean isChanged(long baseLine, long current) {
        return Math.abs(baseLine - current) >= baseLine / 4;
    }

    private void initCacheSize() {
        Function<int[], Long> loopOver = (int[] intArray) -> {
            long time = System.nanoTime();
            int steps = 64 * 1024 * 1024; // Arbitrary number of steps
            int lengthMod = intArray.length - 1;
            for (int i = 0; i < steps; i++)
                intArray[(i * 16) & lengthMod]++; // (x & lengthMod) is equal to (x % arr.Length)
            return System.nanoTime() - time;
        };

        long baseLine = loopOver.apply(new int[128]);
        for (int k=1024; k<32*1024*1024; k *= 2) {
            long current = loopOver.apply(new int[k]);
            logger.finest(k + ": " + current);
            if (isChanged(baseLine, current)) {
                logger.fine("next cache size: " + k / 2 * 4);
                caches.add(k / 2 * 4);
                baseLine = current;
            }
        }
    }

    private void initCPUCores() {
        IntFunction<Long> loopByThreads = (int n) -> {
            Runnable r = () -> {
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

            long time = System.nanoTime();
            for (int i=0; i<n; i++)
                threads[i].start();

            try {
                for (int i = 0; i < n; i++)
                    threads[i].join();
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie.getMessage(), ie);
            }
            return System.nanoTime() - time;
        };

        long baseLine = loopByThreads.apply(1);
        logger.finest(1 + ": " + baseLine);
        for (int k=1; k<32; k *= 2) {
            long current = loopByThreads.apply(k);
            logger.finest(k + ": " + current);
            if (isChanged(baseLine, current)) {
                logger.info("cpu cores: " + k / 2);
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

/*
    public int coreCount() {...}
    public int isSMP() {...}
    public int isNUMA() {...}
*/

    public static void main(String[] args) {
        HardwareSpy hardwareSpy = new HardwareSpy();
        logger.info("CPU cache line size: " + hardwareSpy.cacheLineSize());
        logger.info("CPU L1 cache size: " + hardwareSpy.cacheL1Size());
        logger.info("CPU L2 cache size: " + hardwareSpy.cacheL2Size());
        logger.info("CPU L3 cache size: " + hardwareSpy.cacheL3Size());
    }
}