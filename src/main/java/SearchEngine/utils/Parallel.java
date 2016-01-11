package SearchEngine.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by norman on 10.01.16.
 */
public class Parallel {

    public static <T> void parallelFor(Iterable<T> source, Consumer<T> func) {
        parallelFor(source, func, Runtime.getRuntime().availableProcessors());
    }

    public static <T> void parallelFor(Iterable<T> source, Consumer<T> func, int threads) {
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            for (final T item : source) {
                exec.submit(() -> func.accept(item));
            }
        } finally {
            exec.shutdown();
            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
