package SearchEngine.SaxImporter;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by norman on 19.10.15.
 */
public class SaxImporter {

    public static void readDocNumberAndTitle(File xmlFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            InputStream xmlStream = new FileInputStream(xmlFile);
            saxParser.parse(xmlStream, new DocNumberAndTitleHandler());

        } catch (ParserConfigurationException|IOException|SAXException e) {
            e.printStackTrace();
        }
    }

    public static void readDocNumberFromGzip(File gzipFile) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            InputStream xmlStream = new GZIPInputStream(new FileInputStream(gzipFile));
            saxParser.parse(xmlStream, new DocNumberAndTitleHandler());
            xmlStream.close();
        } catch (ParserConfigurationException|IOException|SAXException e) {
            e.printStackTrace();
        }

    }
}
