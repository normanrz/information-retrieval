package SearchEngine.InvertedIndex;

import SearchEngine.InvertedIndex.disk.DiskInvertedIndex;
import SearchEngine.InvertedIndex.seeklist.EntryListSeekList;
import SearchEngine.InvertedIndex.seeklist.SeekListEntry;
import SearchEngine.InvertedIndex.seeklist.SeekListWriter;

import java.io.*;
import java.util.*;
import java.util.function.Function;
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

        System.out.println("Read seeklists");
        List<DiskInvertedIndex> indexes = new ArrayList<>();
        for (File file : inputIndexFiles) {
            indexes.add(DiskInvertedIndex.withEntryListSeekList(file));
        }

        EntryListSeekList seekList = new EntryListSeekList();

        System.out.println("Collect tokens 1/2");
        List<String> allTokens = indexes.stream()
                .flatMap(DiskInvertedIndex::allTokens)
                .distinct()
                .sorted()
                .collect(Collectors.toList());


        System.out.println("Collect tokens 2/2");
        List<List<DiskInvertedIndex>> tokenMap = allTokens.stream()
//                .peek(System.out::println)
                .map(token -> indexes.stream()
                        .filter(index -> index.has(token))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());


        // Write postings
        File postingsFile = new File(outputFile.getPath() + ".postings");
        try (OutputStream postingsFileStream = new BufferedOutputStream(new FileOutputStream(postingsFile))) {
            long byteCounter = 0;
            for (int i = 0; i < allTokens.size(); i++) {
                String token = allTokens.get(i);
                System.out.println(String.format("Read  %s %d", token, byteCounter));
                // Assumption: One document is only present in strictly one input index
                List<DocumentPostings> postings = tokenMap.get(i).stream()
                        .flatMap(index -> index.get(token))
                        .sorted()
                        .collect(Collectors.toList());

                int tokenCount = postings.stream()
                        .mapToInt(DocumentPostings::getTokenCount)
                        .sum();

                System.out.println(String.format("Write %s %d %d", token, byteCounter, tokenCount));
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
        System.out.println("Write seeklist");
        File seekListFile = new File(outputFile.getPath() + ".seeklist");
        try (DataOutputStream seekListFileStream =
                     new DataOutputStream(new BufferedOutputStream(new FileOutputStream(seekListFile)))) {
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
        System.out.println("Combine inverted index files");
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
