package com.normanrz.SearchEngine.utils;

import java.util.function.Supplier;

/**
 * Created by norman on 29.01.16.
 */
public class RunUtils {

    private RunUtils() {
    }

    public static void runTimedBlock(Runnable action, String label) {
        System.out.println();
        System.out.println("-----------------------------------------------------");
        System.out.println(label);

        long start = System.currentTimeMillis();
        try {
            action.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;

        System.out.println(String.format("%s Time:\t%d\tms", label, time));
        System.out.println("-----------------------------------------------------");
    }

    public static <T> T runTimed(Supplier<T> action, String label) {
        T result = null;
        long start = System.currentTimeMillis();
        try {
            result = action.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long time = System.currentTimeMillis() - start;

        System.out.println(String.format("%s Time:\t%d\tms", label, time));

        return result;
    }
}
