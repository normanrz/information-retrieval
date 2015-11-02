package SearchEngine;

import java.io.*;
import java.util.Queue;
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
                byte[] termBytes = term.getBytes("UTF8");
                outputStream.writeInt(termBytes.length);
                outputStream.write(termBytes);

                Queue<Posting> postingsList = index.get(term);
                outputStream.writeInt(postingsList.size());
                for (Posting posting : postingsList) {
                    outputStream.writeLong(posting.doc_id());
                    outputStream.writeInt(posting.pos());
                }
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
                    int termLength = stream.readInt();
                    byte[] termBytes = new byte[termLength];
                    stream.readFully(termBytes);
                    String term = new String(termBytes, "UTF8");

                    int postingsListLength = stream.readInt();
                    for (int i = 0; i < postingsListLength; i++) {
                        long docNumber = stream.readLong();
                        int pos = stream.readInt();

                        Posting posting = new Posting(docNumber, pos);
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
