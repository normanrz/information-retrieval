package com.normanrz.SearchEngine.DocumentIndex;

import com.normanrz.SearchEngine.Import.XmlPatentReader;
import com.normanrz.SearchEngine.InvertedIndex.TermReader;
import com.normanrz.SearchEngine.InvertedIndex.TermWriter;
import com.normanrz.SearchEngine.PatentDocument;
import org.apache.commons.collections4.map.LRUMap;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by norman on 30.11.15.
 */
public class XmlDocumentIndex implements DocumentIndex {

    private final String directory;
    private final int LRU_CACHE_SIZE = 100;
    private SortedMap<Integer, XmlDocumentIndexEntry> map = new TreeMap<>();
    private LRUMap<XmlDocumentIndexEntry, PatentDocument> lruDocumentCache = new LRUMap<>(LRU_CACHE_SIZE);

    public XmlDocumentIndex(String directory) {
        this.directory = directory;
    }

    private static List<XmlDocumentIndexEntry> loadEntries(InputStream inputStream) throws IOException {
        DataInputStream dataInput = new DataInputStream(inputStream);

        List<String> fileNames = TermReader.readTerms(dataInput);
        return XmlDocumentIndexEntryReader.readDocumentIndexEntries(dataInput, fileNames);
    }

    public static XmlDocumentIndex load(String directory, File inputFile) throws IOException {
        try (InputStream inputStream = new GZIPInputStream(new FileInputStream(inputFile))) {
            return load(directory, inputStream);
        }
    }

    public static XmlDocumentIndex load(String directory, InputStream inputStream) throws IOException {
        XmlDocumentIndex index = new XmlDocumentIndex(directory);
        for (XmlDocumentIndexEntry entry : loadEntries(inputStream)) {
            index.map.put(entry.getDocId(), entry);
        }
        return index;
    }

    public static void merge(String directory, List<File> inputIndexFiles, File outputFile)
            throws IOException, InterruptedException {

        if (inputIndexFiles.size() == 1) {
            Files.copy(inputIndexFiles.get(0).toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        XmlDocumentIndex index = new XmlDocumentIndex(directory);
        for (File file : inputIndexFiles) {
            index.map.putAll(load(directory, file).map);
        }

        index.save(outputFile);
    }

    public Optional<XmlDocumentIndexEntry> get(int docId) {
        return Optional.ofNullable(map.get(docId));
    }

    public int getDocumentTokenCount(int docId) {
        return get(docId).map(XmlDocumentIndexEntry::getDocumentTokenCount).orElse(0);
    }

    public int getDocumentTitleTokenCount(int docId) {
        return get(docId).map(XmlDocumentIndexEntry::getTitleTokenCount).orElse(0);
    }

    public double getDocumentPageRank(int docId) {
        return get(docId).map(XmlDocumentIndexEntry::getPageRank).orElse(0.0);
    }

    public List<String> getPatentDocumentTokens(int docId) {

        return getPatentDocument(docId)
                .map(Stream::of).orElse(Stream.empty()) // Optional to Stream conversion (will be Optional.stream in JDK 9)
                .flatMap(PatentDocument::getStemmedTokens)
                .collect(Collectors.toList());
    }

    public Optional<PatentDocument> getPatentDocument(int docId) {
        return get(docId).flatMap(this::loadPatentDocument);
    }

    public Optional<String> getPatentDocumentTitle(int docId) {
        return getPatentDocument(docId).map(PatentDocument::getTitle);
    }

    private Optional<PatentDocument> loadPatentDocument(XmlDocumentIndexEntry entry) {
        if (lruDocumentCache.containsKey(entry)) {
            return Optional.ofNullable(lruDocumentCache.get(entry));
        }

        try {
//            System.out.println(String.format("[XmlDocumentIndex] Load document %08d from %s at %d",
//                    entry.getDocId(), entry.getFilename(), entry.getOffset()));
            File xmlFile = new File(directory, entry.getFilename());
            InputStream inputStream = Channels.newInputStream(
                    new FileInputStream(xmlFile).getChannel().position(entry.getOffset()));

            Optional<PatentDocument> doc = XmlPatentReader.readSingle(inputStream);
            inputStream.close();
            if (doc.isPresent()) {
                assert doc.get().getDocId() == entry.getDocId() :
                        String.format("%d %d %s", doc.get().getDocId(), entry.getDocId(), entry.getFilename());
                lruDocumentCache.put(entry, doc.get());
            }
            return doc;
        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public int getDocumentCount() {
        return map.size();
    }

    public String getDirectory() {
        return directory;
    }

    public IntStream allDocIds() {
        return map.keySet().stream().mapToInt(Integer::intValue);
    }

    public Stream<Map.Entry<Integer, XmlDocumentIndexEntry>> allEntries() {
        return map.entrySet().stream();
    }

    public void add(int docId, int titleTokenCount, int documentTokenCount, long offset, String fileName) {
        add(docId, titleTokenCount, documentTokenCount, offset, fileName, 0);
    }

    public synchronized void add(
            int docId, int titleTokenCount, int documentTokenCount, long offset, String fileName, double pageRank) {
        XmlDocumentIndexEntry entry =
                new XmlDocumentIndexEntry(docId, titleTokenCount, documentTokenCount, fileName, offset, pageRank);
        map.put(docId, entry);
    }

    public void save(File outputFile) throws IOException {
        try (OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(outputFile))) {
            save(outputStream);
        }
    }

    public void save(OutputStream outputStream) throws IOException {

        DataOutputStream dataOutput = new DataOutputStream(outputStream);

        List<String> fileNames = map.values().stream()
                .map(XmlDocumentIndexEntry::getFilename)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        TermWriter.writeTerms(dataOutput, fileNames);

        XmlDocumentIndexEntryWriter.writeDocumentIndexEntries(dataOutput, map.values(), fileNames);
    }

}
