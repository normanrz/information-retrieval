package SearchEngine.Index;

import SearchEngine.DocumentPostings;
import SearchEngine.Index.disk.SeekList;
import SearchEngine.Index.disk.SeekListEntry;
import SearchEngine.Index.disk.SeekListReader;
import SearchEngine.Index.disk.SeekListWriter;
import SearchEngine.PatentDocument;
import SearchEngine.utils.IntArrayUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by norman on 02.11.15.
 */
public class MemoryPostingIndex extends MemoryIndex<DocumentPostings> implements PostingIndex {

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
        putPosting(token, doc.docId, pos);
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

    public int collectionTokenCount(String token) {
        return get(token)
                .mapToInt(DocumentPostings::tokenFrequency)
                .sum();
    }

    public int documentTokenCount(String token, int docId) {
        return get(token, docId)
                .map(DocumentPostings::tokenFrequency)
                .orElse(0);
    }

    public void save(File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            save(outputStream);
        }
    }

    public void save(OutputStream outputStream) throws IOException {

        SeekList seekList = new SeekList();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int byteCounter = 0;
        for (String token : index.navigableKeySet()) {
            ByteArrayOutputStream postingsBuffer = new ByteArrayOutputStream();
            DataOutputStream postingsDataOutput = new DataOutputStream(new DeflaterOutputStream(postingsBuffer));
            PostingWriter.writeDocumentPostingsList(postingsDataOutput, new ArrayList<>(index.get(token)));
            postingsDataOutput.close();
            int length = postingsBuffer.size();
            seekList.add(new SeekListEntry(token, byteCounter, length));
            byteCounter += length;
            postingsBuffer.writeTo(buffer);
        }

        ByteArrayOutputStream seekListBuffer = new ByteArrayOutputStream();
        DataOutputStream seekListDataOutput = new DataOutputStream(new DeflaterOutputStream(seekListBuffer));
        SeekListWriter.writeSeekList(seekListDataOutput, seekList);
        seekListDataOutput.close();

        DataOutputStream fileDataOutput = new DataOutputStream(outputStream);
        fileDataOutput.writeInt(seekListBuffer.size());
        fileDataOutput.writeInt(collectionTokenCount());

        seekListBuffer.writeTo(outputStream);
        buffer.writeTo(outputStream);
    }


    public static MemoryPostingIndex load(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return load(inputStream);
        }
    }

    public static MemoryPostingIndex load(InputStream inputStream) throws IOException {
        MemoryPostingIndex newIndex = new MemoryPostingIndex();

        InputStream fileStream = new BufferedInputStream(inputStream);
        DataInputStream fileDataInput = new DataInputStream(fileStream);

        int seekListByteLength = fileDataInput.readInt();
        fileDataInput.skipBytes(Integer.BYTES);

        byte[] seekListBuffer = new byte[seekListByteLength];
        fileDataInput.readFully(seekListBuffer);

        DataInputStream seekListDataInput = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(seekListBuffer)));
        SeekList seekList = SeekListReader.readSeekList(seekListDataInput);

        for (SeekListEntry entry : seekList) {
            byte[] postingsBuffer = new byte[entry.getLength()];
            fileDataInput.readFully(postingsBuffer);
            DataInputStream postingsDataInput = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(postingsBuffer)));
            PostingReader.readDocumentPostingsList(postingsDataInput)
                    .forEach(documentPostings -> newIndex.put(entry.getToken(), documentPostings));
        }

        return newIndex;
    }


    @Override
    public void printStats() {
        super.printStats();
        System.out.println("Postings in index: " + collectionTokenCount());
    }


}
