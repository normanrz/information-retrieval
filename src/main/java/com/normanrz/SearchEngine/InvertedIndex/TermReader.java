package com.normanrz.SearchEngine.InvertedIndex;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by norman on 03.11.15.
 */
public class TermReader {

    private TermReader() {
    }

    public static String readTerm(DataInput stream) throws IOException {
//        int termLength = stream.readInt();
//        byte[] termBytes = new byte[termLength];
//        stream.readFully(termBytes);
//        return new String(termBytes, "UTF8");
        return stream.readUTF();

    }

    public static List<String> readTerms(DataInput stream) throws IOException {
        int fileNameListLength = stream.readInt();
        List<String> terms = new ArrayList<>(fileNameListLength);

        for (int i = 0; i < fileNameListLength; i++) {
            terms.add(TermReader.readTerm(stream));
        }
        return terms;
    }
}
