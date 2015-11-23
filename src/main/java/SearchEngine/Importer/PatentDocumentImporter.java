package SearchEngine.Importer;

import SearchEngine.Index.DocumentIndex;
import SearchEngine.Index.MemoryPostingIndex;
import SearchEngine.PatentDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by norman on 19.10.15.
 */
public class PatentDocumentImporter {

    public static Stream<PatentDocument> readPatentDocuments(InputStream inputStream) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            PatentDocumentHandler handler = new PatentDocumentHandler();
            saxParser.parse(inputStream, handler);
            inputStream.close();
            return handler.getBufferedStream();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static Stream<PatentDocument> readPatentDocuments(File xmlFile) {
        try {
            InputStream xmlStream = new FileInputStream(xmlFile);
            return readPatentDocuments(xmlStream);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static Stream<PatentDocument> readCompressedPatentDocuments(File xmlFile) {
        try {
            InputStream xmlStream = new GZIPInputStream(new FileInputStream(xmlFile));
            return readPatentDocuments(xmlStream);
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static void importPatentDocument(PatentDocument doc, MemoryPostingIndex index, DocumentIndex documentIndex) {
        AtomicInteger tokenPosition = new AtomicInteger(0);
        ArrayList<String> tokens = new ArrayList<>();

        String tokenizableDocument = (doc.title + " " + doc.abstractText).toLowerCase();
        PatentDocumentPreprocessor.tokenizeWithRegex(tokenizableDocument).stream()
                .filter(PatentDocumentPreprocessor::isNoStopword)
                .forEach(token -> {
                    String stemmedToken = PatentDocumentPreprocessor.stem(token);
                    index.putPosting(stemmedToken, doc, tokenPosition.getAndIncrement());
                    tokens.add(token);
                });

        documentIndex.storePatentDocument(doc);
    }

}
