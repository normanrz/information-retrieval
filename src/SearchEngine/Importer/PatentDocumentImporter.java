package SearchEngine.Importer;

import SearchEngine.PatentDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.*;
import java.util.stream.Stream;

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
        } catch (ParserConfigurationException|IOException|SAXException e) {
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

}
