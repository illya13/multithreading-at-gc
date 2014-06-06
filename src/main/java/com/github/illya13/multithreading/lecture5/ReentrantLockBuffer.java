package com.github.illya13.multithreading.lecture5;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Goetz. Java concurrency in practice. EN - page 189
public class ReentrantLockBuffer<T> {
    protected final Lock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final T[] items;
    private int tail, head, count;

    @SuppressWarnings("unchecked")
    public ReentrantLockBuffer(int size) {
        this.items = (T[]) new Object[size];
    }

    // BLOCKS-UNTIL: notFull
    public void put(T x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();
            }
            items[tail] = x;
            if (++tail == items.length) {
                tail = 0;
            }
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // BLOCKS-UNTIL: notEmpty
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            T x = items[head];
            items[head] = null;
            if (++head == items.length) {
                head = 0;
            }
            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }

    private static class Test {
        public static void main(String[] args) {
            final ReentrantLockBuffer<Character> buffer = new ReentrantLockBuffer<>(5);
            // PRODUCER
            new Thread(() -> {
                for (char c = 'A'; c <= 'Z'; c++) {
                    try {
                        buffer.put(c);
                        System.err.println(c + "->");
                    } catch (InterruptedException ignore) {/*NOP*/}
                }
            }).start();

            // CONSUMER
            new Thread(() -> {
                char c = '\0';
                do {
                    try {
                        c = buffer.take();
                        System.err.println("   ->" + c);
                    } catch (InterruptedException ignore) {/*NOP*/}
                } while (c != 'Z');
            }).start();
        }
    }
}