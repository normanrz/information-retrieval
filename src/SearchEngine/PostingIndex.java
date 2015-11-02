package SearchEngine;

import javafx.geometry.Pos;

import java.io.*;
import java.util.List;
import java.util.Queue;

/**
 * Created by norman on 02.11.15.
 */
public class PostingIndex extends IndexJasperRzepka<Posting> {

    public void save(File file) {
        try {
            DataOutputStream ramFile = new DataOutputStream(new FileOutputStream(file));

            for (String term : index.navigableKeySet()) {
                ramFile.writeShort(term.length());
                ramFile.writeChars(term);

                Queue<Posting> postingsList = index.get(term);
                ramFile.writeInt(postingsList.size());
                System.out.println(term + " " + postingsList.size());
                for (Posting posting : postingsList) {
                    ramFile.writeLong(posting.doc_id());
                    ramFile.writeInt(posting.pos());
                    System.out.println(Long.parseLong(posting.doc().docNumber) + " " + posting.pos());
                }

            }

            ramFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PostingIndex load(File file) {
        PostingIndex newIndex = new PostingIndex();
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(file));

            while (true) {
                try {
                    short termLength = inputStream.readShort();
                    String term = "";
                    for (short i = 0; i < termLength; i++) {
                        term += inputStream.readChar();
                    }

                    int postingsListLength = inputStream.readInt();
                    for (int i = 0; i < postingsListLength; i++) {
                        long docNumber = inputStream.readLong();
                        int pos = inputStream.readInt();

                        Posting posting = new Posting(docNumber, pos);
                        newIndex.put(term, posting);
                    }
                } catch (EOFException eof) {
                    break;
                }
            }

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newIndex;
    }

}
