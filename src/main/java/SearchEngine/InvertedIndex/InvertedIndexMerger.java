package SearchEngine.InvertedIndex;

import SearchEngine.InvertedIndex.disk.DiskInvertedIndex;
import SearchEngine.InvertedIndex.seeklist.EntryListSeekList;
import SearchEngine.InvertedIndex.seeklist.SeekListEntry;
import SearchEngine.InvertedIndex.seeklist.SeekListWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by norman on 03.11.15.
 */
public class InvertedIndexMerger {

    private InvertedIndexMerger() {
    }

    public static void merge(List<String> inputIndexFilenames, String outputFilename) throws IOException, InterruptedException {
        merge(
                inputIndexFilenames.stream().map(File::new).collect(Collectors.toList()),
                new File(outputFilename));
    }

    public static void merge(List<File> inputIndexFiles, File outputFile) throws IOException, InterruptedException {

//        if (inputIndexFiles.size() == 1) {
//            Files.copy(inputIndexFiles.get(0).toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            return;
//        }


        List<DiskInvertedIndex> indexes = new ArrayList<>();
        for (File file : inputIndexFiles) {
            indexes.add(new DiskInvertedIndex(file));
        }

        EntryListSeekList seekList = new EntryListSeekList();

        SortedMap<String, List<DiskInvertedIndex>> tokenMap = new TreeMap<>();
        for (DiskInvertedIndex index : indexes) {
            List<String> tokenList = index.allTokens().distinct().collect(Collectors.toList());
            for (String token : tokenList) {
                if (!tokenMap.containsKey(token)) {
                    tokenMap.put(token, new ArrayList<>());
                }
                tokenMap.get(token).add(index);
            }
        }

        // Write postings
        File postingsFile = new File(outputFile.getPath() + ".postings");
        try (OutputStream postingsFileStream = new FileOutputStream(postingsFile)) {
            long byteCounter = 0;
            for (String token : tokenMap.keySet()) {
                // Assumption: One document is only present in strictly one input index
                List<DocumentPostings> postings = tokenMap.get(token).stream()
                        .flatMap(index -> index.get(token))
                        .sorted()
                        .collect(Collectors.toList());

                int tokenCount = postings.stream()
                        .mapToInt(DocumentPostings::getTokenCount)
                        .sum();

                ByteArrayOutputStream postingsBuffer =
                        PostingWriter.writeDocumentPostingsListToBuffer(postings);
                int length = postingsBuffer.size();
                seekList.add(new SeekListEntry(token, byteCounter, length, tokenCount));
                byteCounter += length;
                postingsBuffer.writeTo(postingsFileStream);
            }
            postingsFileStream.close();
        }


        // Write EntryListSeekList
        File seekListFile = new File(outputFile.getPath() + ".seeklist");
        try (DataOutputStream seekListFileStream =
                     new DataOutputStream(new FileOutputStream(seekListFile))) {
            SeekListWriter.writeSeekList(seekListFileStream, seekList);
            seekListFileStream.close();
        }

        // Write header
        File headerFile = new File(outputFile.getPath() + ".header");
        try (DataOutputStream headerFileStream = new DataOutputStream(new FileOutputStream(headerFile))) {
            headerFileStream.writeInt((int) seekListFile.length());
            headerFileStream.writeInt(indexes.stream().mapToInt(DiskInvertedIndex::getCollectionTokenCount).sum());
            headerFileStream.close();
        }


        // Merge files
        String[] cmd = {
                "/bin/sh",
                "-c",
                "cat " +
                        headerFile.getAbsolutePath() + " " +
                        seekListFile.getAbsolutePath() + " " +
                        postingsFile.getAbsolutePath() + " > " +
                        outputFile.getAbsolutePath()
        };
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();

        // Cleanup
        headerFile.delete();
        seekListFile.delete();
        postingsFile.delete();
        for (DiskInvertedIndex index : indexes) {
            index.close();
        }
    }
}
