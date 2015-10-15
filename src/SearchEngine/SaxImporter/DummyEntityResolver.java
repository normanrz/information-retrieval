package SearchEngine.SaxImporter;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;

/**
 * @author: JasperRzepka
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 *
 * Source: http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds
 */
public class DummyEntityResolver implements EntityResolver {
    public InputSource resolveEntity(String publicID, String systemID)
            throws SAXException {

        return new InputSource(new StringReader(""));
    }
}