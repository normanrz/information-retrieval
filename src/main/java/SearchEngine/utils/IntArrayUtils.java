package SearchEngine.utils;

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

}
