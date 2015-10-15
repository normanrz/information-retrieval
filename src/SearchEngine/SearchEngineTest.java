package SearchEngine;


import java.io.FileReader;
import java.io.IOException;

import SearchEngine.SaxImporter.DummyEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import SearchEngine.SaxImporter.SaxImporter;

/**
 *
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * You can run your search engine using this file
 * You can use/change this file during the development of your search engine.
 * Any changes you make here will be ignored for the final test!
 */

public class SearchEngineTest {
    
    
    public static void main(String args[]) throws Exception {


        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            FileReader reader = new FileReader("data/testData.xml");
            InputSource inputSource = new InputSource(reader);

            xmlReader.setEntityResolver(new DummyEntityResolver());
            xmlReader.setContentHandler(new SaxImporter());
            xmlReader.parse(inputSource);

        } catch (IOException|SAXException e) {
            e.printStackTrace();
        }

        // SearchEngine myEngine = new SearchEngineJasperRzepka();
        
        // long start = System.currentTimeMillis();
        
        // myEngine.index(String directory)
        
        // long time = System.currentTimeMillis() - start;
        
        // System.out.print("Indexing Time:\t" + time + "\tms\n");
        
        // myEngine.loadIndex(String directory)
        
        // String query = "";
        
        // ArrayList <String> results = new ArrayList <> ();
        
        // results = myEngine.search(String query, int topK, int prf)
        
    }

}
