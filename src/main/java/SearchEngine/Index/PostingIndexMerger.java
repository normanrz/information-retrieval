package SearchEngine.Index;

import SearchEngine.DocumentPostings;
import SearchEngine.Posting;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by norman on 03.11.15.
 */
public class PostingIndexMerger {


    public void merge(List<File> inputIndexFiles, File outputIndexFile) throws IOException {

        List<InputStream> inputStreams = new ArrayList<>(inputIndexFiles.size());
        for (File file : inputIndexFiles) {
            inputStreams.add(new FileInputStream(file));
        }
        merge(inputStreams, new FileOutputStream(outputIndexFile));

    }

    public void mergeCompressed(List<File> inputIndexFiles, File outputIndexFile) throws IOException {

        List<InputStream> inputStreams = new ArrayList<>(inputIndexFiles.size());
        for (File file : inputIndexFiles) {
            inputStreams.add(new GZIPInputStream(new FileInputStream(file)));
        }
        merge(inputStreams, new GZIPOutputStream(new FileOutputStream(outputIndexFile)));

    }

    public void merge(List<InputStream> inputBaseStreams, OutputStream outputBaseStream) throws IOException {

        List<DataInputStream> inputStreams = inputBaseStreams.stream()
                .map(BufferedInputStream::new)
                .map(DataInputStream::new)
                .collect(Collectors.toList());

        DataOutputStream outputStream = new DataOutputStream(outputBaseStream);

        Map<DataInputStream, String> currentTerms = new HashMap<>();

        // Get current term of all streams
        for (DataInputStream stream : inputStreams) {
            try {
                String term = TermReader.readTerm(stream);
                currentTerms.put(stream, term);
            } catch (EOFException e) {
                continue;
            }
        }

        while (!currentTerms.isEmpty()) {

            // Determine minimal term
            String minimalTerm = currentTerms.values().stream().min(String::compareTo).get();

            // Write term to output
            TermWriter.writeTerm(outputStream, minimalTerm);

            // Filter streams with that term
            List<DataInputStream> relevantStreams =
                    currentTerms.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(minimalTerm))
                            .map(entry -> entry.getKey())
                            .collect(Collectors.toList());

            // Merge sort their posting lists
            List<DocumentPostings> mergedPostingsList =
                    relevantStreams.stream()
                            .map(stream -> {
                                try {
                                    return PostingReader.readDocumentPostingsList(stream);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .filter(a -> a != null)
                            .flatMap(a -> a.stream())
                            .sorted()
                            .collect(Collectors.toList());

            // Write merged posting list to output
            PostingWriter.writeDocumentPostingsList(outputStream, mergedPostingsList);

            // Get new current term or close/remove stream if EOF
            for (DataInputStream stream : relevantStreams) {
                try {
                    String term = TermReader.readTerm(stream);
                    currentTerms.put(stream, term);
                } catch (EOFException e) {
                    currentTerms.remove(stream);
                    stream.close();
                }
            }

        }

        outputStream.close();

    }
}
