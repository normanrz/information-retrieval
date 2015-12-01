package SearchEngine.Importer;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.DocumentIndex.XmlPatentReader;
import SearchEngine.InvertedIndex.memory.MemoryInvertedIndex;
import SearchEngine.PatentDocument;
import com.twitter.elephantbird.util.StreamSearcher;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.collections.primitives.LongList;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            return XmlPatentReader.readMultiple(xmlStream);
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
        try (InputStream inputStream = new FileInputStream(inputFile)) {
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


    public static void importPatentDocuments(File file, MemoryInvertedIndex index, XmlDocumentIndex documentIndex) {
        long[] offsets = PatentDocumentImporter.readPatentDocumentOffsets(file);
        List<PatentDocument> patentDocuments = PatentDocumentImporter.readPatentDocuments(file);

        for (int i = 0; i < offsets.length; i++) {
            PatentDocument doc = patentDocuments.get(i);
            long offset = offsets[i];

            AtomicInteger tokenCounter = new AtomicInteger(0);
            doc.getStemmedTokens().forEachOrdered(token ->
                    index.putPosting(token, doc.getDocId(), tokenCounter.getAndIncrement()));
            documentIndex.add(doc.getDocId(), tokenCounter.get(), offset, file.getName());
        }
    }


}
