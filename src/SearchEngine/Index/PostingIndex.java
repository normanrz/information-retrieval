package SearchEngine.Index;

import SearchEngine.Posting;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by norman on 02.11.15.
 */
public class PostingIndex extends GenericIndex<Posting> {

    public void save(OutputStream stream) {
        try {
            DataOutputStream outputStream = new DataOutputStream(stream);

            for (String term : index.navigableKeySet()) {
                TermWriter.writeTerm(outputStream, term);
                PostingWriter.writePostingsList(outputStream, new ArrayList<>(index.get(term)));
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(File file) {
        try {
            save(new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveCompressed(File file) {
        try {
            save(new GZIPOutputStream(new FileOutputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static PostingIndex load(InputStream inputStream) {
        PostingIndex newIndex = new PostingIndex();
        try {
            DataInputStream stream = new DataInputStream(inputStream);

            int j = 0;
            while (true) {
                try {
                    String term = TermReader.readTerm(stream);
                    for (Posting posting : PostingReader.readPostingsList(stream)) {
                        newIndex.put(term, posting);
                    }
                } catch (EOFException eof) {
                    break;
                }
                j++;
            }

            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newIndex;
    }

    public static PostingIndex loadCompressed(File file) {
        try {
            return load(new GZIPInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PostingIndex load(File file) {
        try {
            return load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
