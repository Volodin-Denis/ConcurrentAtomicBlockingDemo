package com.company;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeInventorySystem {
    private final ConcurrentMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final BlockingQueue<String> orderQueue = new LinkedBlockingQueue<>();
    private final AtomicInteger totalSales = new AtomicInteger(0);
    private final Set<String> uniqueCustomers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws InterruptedException {
        ThreadSafeInventorySystem system = new ThreadSafeInventorySystem();
        system.addProduct("Laptop", 5);
        system.addProduct("Smartphone", 10);

        system.processOrders();

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 20; i++) {
            final int customerId = i;
            executorService.submit(() -> {
                try {
                    system.placeOrder("Laptop", "Customer" + customerId);
                    system.placeOrder("Smartphone", "Customer" + customerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        Thread.sleep(1000);
        system.printStatistics();
    }

    public void addProduct(String product, int quantity) {
        inventory.putIfAbsent(product, new AtomicInteger(0));
        inventory.get(product).addAndGet(quantity);
        System.out.println("Added " + quantity + " " + product + " to inventory");
    }

    public void placeOrder(String product, String customer) throws InterruptedException {
        if (inventory.containsKey(product) && inventory.get(product).get() > 0) {
            orderQueue.put(product);
            uniqueCustomers.add(customer);
            System.out.println("Order placed for " + product + " by " + customer);
        } else {
            System.out.println("Product " + product + " is out of stock");
        }
    }

    public void processOrders() {
        new Thread(() -> {
            while (true) {
                try {
                    String product = orderQueue.take();
                    if (inventory.get(product).decrementAndGet() >= 0) {
                        totalSales.incrementAndGet();
                        System.out.println("Processed order for " + product);
                    } else {
                        inventory.get(product).incrementAndGet();
                        System.out.println("Cannot process order for " + product + " - out of stock");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void printStatistics() {
        System.out.println("Total sales: " + totalSales.get());
        System.out.println("Unique customers: " + uniqueCustomers.size());
        System.out.println("Current inventory:");
        inventory.forEach((product, quantity) ->
            System.out.println(product + ": " + quantity.get()));
    }
}
