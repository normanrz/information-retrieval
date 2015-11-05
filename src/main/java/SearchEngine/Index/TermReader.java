package SearchEngine.Index;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by norman on 03.11.15.
 */
public class TermReader {
    public static String readTerm(DataInputStream stream) throws IOException {
        int termLength = stream.readInt();
        byte[] termBytes = new byte[termLength];
        stream.readFully(termBytes);
        return new String(termBytes, "UTF8");
    }
}
