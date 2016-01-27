package SearchEngine.Import;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.InvertedIndex.memory.MemoryInvertedIndex;
import SearchEngine.LinkIndex.LinkIndex;
import SearchEngine.PatentDocument;
import com.twitter.elephantbird.util.StreamSearcher;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.collections.primitives.LongList;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Created by norman on 19.10.15.
 */
public class PatentDocumentImporter {


    public static List<PatentDocument> readPatentDocuments(File xmlFile) {
        try (InputStream xmlStream = new FileInputStream(xmlFile)) {
            return XmlPatentReader.readMultipleWithIndex(xmlStream);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<PatentDocument> readCompressedPatentDocuments(File xmlFile) {
        try (InputStream xmlStream = new GZIPInputStream(new FileInputStream(xmlFile))) {
            return XmlPatentReader.readMultiple(xmlStream);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static long[] readPatentDocumentOffsets(File inputFile) {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile))) {
            LongList list = new ArrayLongList();
            String pattern = "<us-patent-grant";
            StreamSearcher searcher = new StreamSearcher(pattern.getBytes("UTF8"));

            long offset = 0;
            while (true) {
                long delta = searcher.search(inputStream);
                if (delta >= 0) {
                    offset = offset + delta;
                    list.add(offset - pattern.length());
                } else {
                    break;
                }
            }
            return list.toArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new long[0];
        }
    }


    public static void importPatentDocuments(
            File file, MemoryInvertedIndex index, XmlDocumentIndex documentIndex, LinkIndex linkIndex) {
        long[] offsets = PatentDocumentImporter.readPatentDocumentOffsets(file);
        List<PatentDocument> patentDocuments = PatentDocumentImporter.readPatentDocuments(file);

        assert offsets.length == patentDocuments.size() :
                String.format("%s %d %d", file, offsets.length, patentDocuments.size());

        for (int i = 0; i < offsets.length; i++) {
            PatentDocument doc = patentDocuments.get(i);
            if (doc != null) {
                long offset = offsets[i];

                AtomicInteger tokenCounter = new AtomicInteger(0);
                PatentDocumentPreprocessor.preprocess(doc.getTitle()).forEachOrdered(token ->
                                index.putPosting(token, doc.getDocId(), tokenCounter.getAndIncrement())
                );
                int titleTokenCount = tokenCounter.get();
                PatentDocumentPreprocessor.preprocess(doc.getBody()).forEachOrdered(token ->
                                index.putPosting(token, doc.getDocId(), tokenCounter.getAndIncrement())
                );

                documentIndex.add(doc.getDocId(), titleTokenCount, tokenCounter.get(), offset, file.getName());
                for (int citation : doc.getCitations()) {
                    linkIndex.add(citation, doc.getDocId());
                }
            }
        }
    }


}
