package SearchEngine.SaxImporter;

import SearchEngine.PatentDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by norman on 19.10.15.
 */
public class SaxImporter {

    public static Stream<PatentDocument> readDocNumberAndTitle(File xmlFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            InputStream xmlStream = new FileInputStream(xmlFile);
            DocNumberAndAbstractHandler handler = new DocNumberAndAbstractHandler();
            saxParser.parse(xmlStream, handler);
            xmlStream.close();
            return handler.getBuffer();
        } catch (ParserConfigurationException|IOException|SAXException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    public static Stream<PatentDocument> readDocNumberFromGzip(File gzipFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            InputStream xmlStream = new GZIPInputStream(new FileInputStream(gzipFile));
            DocNumberAndAbstractHandler handler = new DocNumberAndAbstractHandler();
            saxParser.parse(xmlStream, handler);
            xmlStream.close();
            return handler.getBuffer();
        } catch (ParserConfigurationException|IOException|SAXException e) {
            e.printStackTrace();
            return Stream.empty();
        }

    }
}
