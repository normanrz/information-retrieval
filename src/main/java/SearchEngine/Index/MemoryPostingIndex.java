package SearchEngine.Index;

import SearchEngine.DocumentPostings;
import SearchEngine.PatentDocument;
import SearchEngine.utils.IntArrayUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by norman on 02.11.15.
 */
public class MemoryPostingIndex extends GenericIndex<DocumentPostings> implements PostingIndex {

    public Optional<DocumentPostings> get(String token, int docId) {
        return get(token)
                .filter(documentPostings -> documentPostings.docId() == docId)
                .findFirst();
    }

    public Stream<DocumentPostings> getInDocs(String token, int[] docIds) {
        return get(token)
                .filter(posting -> IntArrayUtils.intArrayContains(docIds, posting.docId()));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return getByPrefix(token)
                .filter(posting -> IntArrayUtils.intArrayContains(docIds, posting.docId()));
    }

    public void putPosting(String token, PatentDocument doc, int pos) {
        putPosting(token, Integer.parseInt(doc.docNumber), pos);
    }

    public void putPosting(String token, int docId, int pos) {
        ConcurrentLinkedQueue<DocumentPostings> postingsList = index.get(token);
        if (postingsList == null) {
            put(token, new DocumentPostings(docId, pos));
        } else {
            Optional<DocumentPostings> documentPostings = get(token, docId);
            if (documentPostings.isPresent()) {
                documentPostings.get().addPosition(pos);
            } else {
                put(token, new DocumentPostings(docId, pos));
            }
        }
    }


    public int collectionTokenCount() {
        return all().mapToInt(DocumentPostings::tokenFrequency)
                .sum();
    }

    public int documentTokenCount(int docId) {
        return all().filter(documentPostings -> documentPostings.docId() == docId)
                .mapToInt(DocumentPostings::tokenFrequency)
                .sum();
    }

    public int collectionTokenFrequency(String token) {
        return get(token)
                .mapToInt(DocumentPostings::tokenFrequency)
                .sum();
    }

    public int documentTokenFrequency(String token, int docId) {
        return get(token, docId)
                .map(DocumentPostings::tokenFrequency)
                .orElse(0);
    }


    public void save(OutputStream stream) {
        try {
            DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(stream));

            for (String term : index.navigableKeySet()) {
                TermWriter.writeTerm(outputStream, term);
                PostingWriter.writeDocumentPostingsList(outputStream, new ArrayList<>(index.get(term)));
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


    public static MemoryPostingIndex load(InputStream inputStream) {
        MemoryPostingIndex newIndex = new MemoryPostingIndex();
        try {
            DataInputStream stream = new DataInputStream(new BufferedInputStream(inputStream));

            while (true) {
                try {
                    String term = TermReader.readTerm(stream);
                    for (DocumentPostings documentPostings : PostingReader.readDocumentPostingsList(stream)) {
                        newIndex.put(term, documentPostings);
                    }
                } catch (EOFException eof) {
                    break;
                }
            }

            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newIndex;
    }

    public static MemoryPostingIndex loadCompressed(File file) {
        try {
            return load(new GZIPInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MemoryPostingIndex load(File file) {
        try {
            return load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void printStats() {
        super.printStats();
        System.out.println("Postings in index: " + collectionTokenCount());
    }


}
