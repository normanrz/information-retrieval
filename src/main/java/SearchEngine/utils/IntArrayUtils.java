package SearchEngine.utils;

import java.util.Arrays;

/**
 * Created by norman on 13.11.15.
 */
public class IntArrayUtils {

    public static boolean intArrayContains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }

    public static int[] intersection(final int[] a, final int[] b) {
        return Arrays.stream(a)
                .filter(a_item -> intArrayContains(b, a_item))
                .toArray();
    }

}
