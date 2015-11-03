package SearchEngine;

import javafx.geometry.Pos;

import javax.imageio.IIOException;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by norman on 03.11.15.
 */
public class MergePostingIndex {

    private String readTerm(DataInputStream stream) throws IOException {
        int termLength = stream.readInt();
        byte[] termBytes = new byte[termLength];
        stream.readFully(termBytes);
        return new String(termBytes, "UTF8");
    }

    private Posting readPosting(DataInputStream stream) throws IOException {
        long docNumber = stream.readLong();
        int pos = stream.readInt();
        return new Posting(docNumber, pos);
    }

    private Collection<Posting> readPostingsList(DataInputStream stream) throws IOException {
        int postingsListLength = stream.readInt();
        Collection<Posting> postingsList = new ArrayList<>(postingsListLength);
        for (int i = 0; i < postingsListLength; i++) {
            postingsList.add(readPosting(stream));
        }
        return postingsList;
    }

    private void writeTerm(DataOutputStream stream, String term) throws IOException {
        byte[] termBytes = term.getBytes("UTF8");
        stream.writeInt(termBytes.length);
        stream.write(termBytes);
    }

    private void writePosting(DataOutputStream stream, Posting posting) throws IOException {
        stream.writeLong(posting.docId());
        stream.writeInt(posting.pos());
    }

    private void writePostingsList(DataOutputStream stream, Collection<Posting> postingsList) throws IOException {
        stream.writeInt(postingsList.size());
        for (Posting posting : postingsList) {
            writePosting(stream, posting);
        }
    }



    public void merge(Collection<File> inputIndexFiles, File outputIndexFile) throws IOException {

        Collection<InputStream> inputStreams = new ArrayList<>(inputIndexFiles.size());
        for (File file : inputIndexFiles) {
            inputStreams.add(new FileInputStream(file));
        }
        merge(inputStreams, new FileOutputStream(outputIndexFile));

    }

    public void mergeCompressed(Collection<File> inputIndexFiles, File outputIndexFile) throws IOException {

        Collection<InputStream> inputStreams = new ArrayList<>(inputIndexFiles.size());
        for (File file : inputIndexFiles) {
            inputStreams.add(new GZIPInputStream(new FileInputStream(file)));
        }
        merge(inputStreams, new GZIPOutputStream(new FileOutputStream(outputIndexFile)));

    }

    public void merge(Collection<InputStream> inputBaseStreams, OutputStream outputBaseStream) throws IOException {

        Collection<DataInputStream> inputStreams = new ArrayList<>(inputBaseStreams.size());
        for (InputStream baseStream : inputBaseStreams) {
            inputStreams.add(new DataInputStream(baseStream));
        }

        DataOutputStream outputStream = new DataOutputStream(outputBaseStream);

        Map<DataInputStream, String> currentTerms = new HashMap<>();

        // Get current term of all streams
        for (DataInputStream stream : inputStreams) {
            try {
                String term = readTerm(stream);
                currentTerms.put(stream, term);
            } catch (EOFException e) {
                continue;
            }
        }

        while (!currentTerms.isEmpty()) {

            // Determine minimal term
            String minimalTerm = currentTerms.values().stream().min(String::compareTo).get();

            // Write term to output
            writeTerm(outputStream, minimalTerm);

            // Filter streams with that term
            Collection<DataInputStream> relevantStreams =
                    currentTerms.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(minimalTerm))
                            .map(entry -> entry.getKey())
                            .collect(Collectors.toList());

            // Merge sort their posting lists
            Collection<Posting> mergedPostingsList =
                    relevantStreams.stream()
                            .map(stream -> {
                                try {
                                    return readPostingsList(stream);
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
            writePostingsList(outputStream, mergedPostingsList);

            // Get new current term or close/remove stream if EOF
            for (DataInputStream stream : relevantStreams) {
                try {
                    String term = readTerm(stream);
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
