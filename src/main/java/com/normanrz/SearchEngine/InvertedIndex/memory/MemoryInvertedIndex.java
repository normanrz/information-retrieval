package com.normanrz.SearchEngine.InvertedIndex.memory;

import com.normanrz.SearchEngine.InvertedIndex.DocumentPostings;
import com.normanrz.SearchEngine.InvertedIndex.InvertedIndex;
import com.normanrz.SearchEngine.InvertedIndex.PostingReader;
import com.normanrz.SearchEngine.InvertedIndex.PostingWriter;
import com.normanrz.SearchEngine.InvertedIndex.seeklist.EntryListSeekList;
import com.normanrz.SearchEngine.InvertedIndex.seeklist.SeekListEntry;
import com.normanrz.SearchEngine.InvertedIndex.seeklist.SeekListReader;
import com.normanrz.SearchEngine.InvertedIndex.seeklist.SeekListWriter;
import com.normanrz.SearchEngine.PatentDocument;
import com.normanrz.SearchEngine.utils.IntArrayUtils;

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
public class MemoryInvertedIndex extends MemoryIndex<DocumentPostings> implements InvertedIndex {

    public static MemoryInvertedIndex load(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return load(inputStream);
        }
    }

    public static MemoryInvertedIndex load(InputStream inputStream) throws IOException {
        MemoryInvertedIndex newIndex = new MemoryInvertedIndex();

        InputStream fileStream = new BufferedInputStream(inputStream);
        DataInputStream fileDataInput = new DataInputStream(fileStream);

        int seekListByteLength = fileDataInput.readInt();
        fileDataInput.skipBytes(Integer.BYTES); // Header

        EntryListSeekList seekList = SeekListReader.readSeekListFromFile(fileDataInput, seekListByteLength);

        for (SeekListEntry entry : seekList) {
            byte[] postingsBuffer = new byte[entry.getLength()];
            fileDataInput.readFully(postingsBuffer);
            DataInputStream postingsDataInput = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(postingsBuffer)));
            PostingReader.readDocumentPostingsList(postingsDataInput)
                    .forEach(documentPostings -> newIndex.put(entry.getToken(), documentPostings));
        }

        return newIndex;
    }

    public Optional<DocumentPostings> get(String token, int docId) {
        return get(token)
                .filter(documentPostings -> documentPostings.getDocId() == docId)
                .findFirst();
    }

    public Stream<DocumentPostings> getInDocs(String token, int[] docIds) {
        return get(token)
                .filter(posting -> IntArrayUtils.intArrayContains(docIds, posting.getDocId()));
    }

    public Stream<DocumentPostings> getByPrefixInDocs(String token, int[] docIds) {
        return getByPrefix(token)
                .filter(posting -> IntArrayUtils.intArrayContains(docIds, posting.getDocId()));
    }

    public void putPosting(String token, PatentDocument doc, int pos) {
        putPosting(token, doc.getDocId(), pos);
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

    public int getCollectionTokenCount() {
        return all().mapToInt(DocumentPostings::getTokenCount)
                .sum();
    }

    public int getCollectionTokenCount(String token) {
        return get(token)
                .mapToInt(DocumentPostings::getTokenCount)
                .sum();
    }

    public int getDocumentTokenCount(String token, int docId) {
        return get(token, docId)
                .map(DocumentPostings::getTokenCount)
                .orElse(0);
    }

    public int getDocumentTitleTokenCount(String token, int docTitleTokenCount, int docId) {
        return get(token, docId)
                .map(documentPostings -> documentPostings.getTitleTokenCount(docTitleTokenCount))
                .orElse(0);
    }

    public void save(File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            save(outputStream);
        }
    }

    public void save(OutputStream outputStream) throws IOException {

        EntryListSeekList seekList = new EntryListSeekList();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        long byteCounter = 0;
        for (String token : index.navigableKeySet()) {
            ByteArrayOutputStream postingsBuffer = new ByteArrayOutputStream();
            DataOutputStream postingsDataOutput = new DataOutputStream(new DeflaterOutputStream(postingsBuffer));
            PostingWriter.writeDocumentPostingsList(postingsDataOutput, new ArrayList<>(index.get(token)));
            postingsDataOutput.close();
            int length = postingsBuffer.size();
            seekList.add(new SeekListEntry(token, byteCounter, length, getCollectionTokenCount(token)));
            byteCounter += length;
            postingsBuffer.writeTo(buffer);
        }

        ByteArrayOutputStream seekListBuffer = new ByteArrayOutputStream();
        DataOutputStream seekListDataOutput = new DataOutputStream(seekListBuffer);
        SeekListWriter.writeSeekList(seekListDataOutput, seekList);
        seekListDataOutput.close();

        DataOutputStream fileDataOutput = new DataOutputStream(outputStream);
        fileDataOutput.writeInt(seekListBuffer.size());
        fileDataOutput.writeInt(getCollectionTokenCount());

        seekListBuffer.writeTo(outputStream);
        buffer.writeTo(outputStream);
    }

    @Override
    public void printStats() {
        super.printStats();
        System.out.println("Postings in index: " + getCollectionTokenCount());
    }


}
