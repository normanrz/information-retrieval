package SearchEngine.utils;

/**
 * Created by norman on 26.11.15.
 */
public class ArrayUtils<T> {

    public static <T> T first(T[] array) {
        return array[0];
    }

    public static int first(int[] array) {
        return array[0];
    }

    public static <T> T last(T[] array) {
        return array[array.length - 1];
    }

    public static int last(int[] array) {
        return array[array.length - 1];
    }
}
