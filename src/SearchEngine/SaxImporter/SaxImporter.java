package SearchEngine.SaxImporter;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author: JasperRzepka
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 */
public class SaxImporter extends DefaultHandler {

    private String inventionTitle = "";
    private String docNumber = "";
    private StringBuffer currentBuffer;
    private Boolean isInPublicationReference = false;

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {

        currentBuffer = new StringBuffer();
        switch (localName) {
            case "publication-reference":
                isInPublicationReference = true;
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String currentValue = currentBuffer.toString().replaceAll("\\s+", " ");
        switch (localName) {
            case "publication-reference":
                isInPublicationReference = false;
                break;
            case "us-patent-grant":
                System.out.println(docNumber + ": " + inventionTitle);
                break;
            case "invention-title":
                inventionTitle = currentValue;
                break;
            case "doc-number":
                if (isInPublicationReference) {
                    docNumber = currentValue;
                }
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentBuffer.append(new String(ch, start, length));
    }

}
