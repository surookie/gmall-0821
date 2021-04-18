package com.atguigu.gmall.item.service.impl;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @Description 异步编排技术
 * @Author rookie
 * @Date 2021/4/15 22:12
 */
public class CompletableFutureTest {
    public static void main(String[] args) {
        /*CompletableFuture<Void> futures = CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                System.out.println("hello completableFuture....");
            }
        });
        try {
            futures.get();
            System.out.println("This is the main thread method. . . . Better not stop me");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }*/

        CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                return "这是CompletableFuture异步线程。。。。";
            }
        });
        future.thenApplyAsync(t -> {
            System.out.println("------------thenApplyAsync 1-----------");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上个任务返回结果1：" + t);
            return "hello thenApplyAsyn1...";
        });
        future.thenApplyAsync(t -> {
            System.out.println("------------thenApplyAsync 2-----------");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上个任务返回结果2：" + t);
            return "hello thenApplyAsyn2...";
        });
        future.thenApplyAsync(t -> {
            System.out.println("------------thenApplyAsync 3-----------");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上个任务返回结果3：" + t);
            return "hello thenApplyAsyn3...";
        });
        future.whenCompleteAsync((t, u) -> {
            System.out.println("t:" + t);
            System.out.println("u:" + u);
        });

                /*.whenCompleteAsync((t, u) -> {
            System.out.println("t = " + t);
            System.out.println("u = " + u);

        }).exceptionally(t-> {
            System.out.println("异常信息。。。。" + t);
            System.out.println("异常任务处理");
            return "hello exceptionally";
        });*/

        try {
            System.out.println("这是主线程方法执行。。。。");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class testCallable {
    public static void main(String[] args) {
        FutureTask<String> futureTask = new FutureTask<String>(new CallableTest());
        new Thread(futureTask).start();
        try {
            System.out.println("futureTask.get() = " + futureTask.get());
            System.out.println("This is the main thread method. . . . Better not stop me");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}

class CallableTest implements Callable {

    @Override
    public Object call() throws Exception {
        System.out.println("callable.............");
        return "Hello Callable.....";
    }
}