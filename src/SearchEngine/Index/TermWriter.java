package SearchEngine.Index;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by norman on 03.11.15.
 */
public class TermWriter {

    public static void writeTerm(DataOutputStream stream, String term) throws IOException {
        byte[] termBytes = term.getBytes("UTF8");
        stream.writeInt(termBytes.length);
        stream.write(termBytes);
    }
}
