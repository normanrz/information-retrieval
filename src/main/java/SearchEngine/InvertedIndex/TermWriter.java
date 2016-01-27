package SearchEngine.InvertedIndex;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by norman on 03.11.15.
 */
public class TermWriter {

    public static void writeTerm(DataOutput stream, String term) throws IOException {
//        byte[] termBytes = term.getBytes("UTF8");
//        stream.writeInt(termBytes.length);
//        stream.write(termBytes);
        stream.writeUTF(term);
    }

    public static void writeTerms(DataOutput stream, Collection<String> terms) throws IOException {
        stream.writeInt(terms.size());
        for (String term : terms) {
            TermWriter.writeTerm(stream, term);
        }
    }
}
