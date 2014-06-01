package com.github.illya13.multithreading.lecture2;

public class PetersonLockDemo {
    public static void main(String[] args) {
        final PetersonLock lock = new PetersonLock();
        new Thread(() -> {
            while (true) {
                lock.lockA();
                try {
                    System.out.println("A");
                } finally {
                    lock.unlockA();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                lock.lockB();
                try {
                    System.out.println("    B");
                } finally {
                    lock.unlockB();
                }
            }
        }).start();
    }
}
