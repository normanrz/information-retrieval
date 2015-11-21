package SearchEngine.Index;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by norman on 03.11.15.
 */
public class TermWriter {

    public static void writeTerm(DataOutputStream stream, String term) throws IOException {
        byte[] termBytes = term.getBytes("UTF8");
        stream.writeInt(termBytes.length);
        stream.write(termBytes);
    }

    public static int termByteLength(String term) {
        try {
            return Integer.BYTES + term.getBytes("UTF8").length;
        } catch (UnsupportedEncodingException e) {
            return 0;
        }

    }
}
