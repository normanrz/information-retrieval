package SearchEngine.LinkIndex;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Created by norman on 25.01.16.
 */
public class LinkIndex {

    private static int[] emptyIntArray = new int[0];
    private SortedMap<Integer, IntList> list;

    public LinkIndex() {
        this(new TreeMap<>());
    }

    public LinkIndex(SortedMap<Integer, IntList> list) {
        this.list = list;
    }

    public synchronized void add(int docId, int citedByDocId) {
        Optional<IntList> entryOption = getEntry(docId);
        if (entryOption.isPresent()) {
            entryOption.get().add(citedByDocId);
        } else {
            IntList intList = new ArrayIntList();
            intList.add(citedByDocId);
            list.put(docId, intList);
        }
    }

    Optional<IntList> getEntry(int docId) {
        return Optional.ofNullable(list.get(docId));
    }

    public synchronized int[] get(int docId) {
        return getEntry(docId)
                .map(IntList::toArray)
                .orElse(emptyIntArray);
    }

    public static LinkIndex load(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return load(inputStream);
        }
    }

    public static LinkIndex load(InputStream inputStream) throws IOException {
        InputStream fileStream = new BufferedInputStream(inputStream);
        DataInputStream fileDataInput = new DataInputStream(fileStream);

        int length = fileDataInput.readInt();
        SortedMap<Integer, IntList> list = new TreeMap<>();
        for (int i = 0; i < length; i++) {
            int docId = fileDataInput.readInt();
            int citesLength = fileDataInput.readInt();
            IntList cites = new ArrayIntList(citesLength);
            for (int j = 0; j < citesLength; j++) {
                cites.add(fileDataInput.readInt());
            }
            list.put(docId, cites);
        }
        return new LinkIndex(list);
    }

    public void save(File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            save(outputStream);
        }
    }

    public void save(OutputStream outputStream) throws IOException {
        DataOutputStream fileDataOutput = new DataOutputStream(outputStream);

        fileDataOutput.writeInt(list.size());
        for (Map.Entry<Integer, IntList> entry : list.entrySet()) {
            fileDataOutput.writeInt(entry.getKey());
            fileDataOutput.writeInt(entry.getValue().size());
            for (int citedBy : entry.getValue().toArray()) {
                fileDataOutput.writeInt(citedBy);
            }
        }
    }

    public static void merge(List<File> inputIndexFiles, File outputFile)
            throws IOException, InterruptedException {

        if (inputIndexFiles.size() == 1) {
            Files.copy(inputIndexFiles.get(0).toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        LinkIndex index = new LinkIndex();
        for (File file : inputIndexFiles) {
            index.list.putAll(load(file).list);
        }

        index.save(outputFile);
    }
}
