package SearchEngine.SaxImporter;

import SearchEngine.DocNumberAndAbstract;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;


/**
 * @author: JasperRzepka
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 */
public class DocNumberAndAbstractHandler extends DefaultHandler {

    private String inventionAbstract = "";
    private String docNumber = "";
    private StringBuffer currentBuffer;
    private Boolean isInPublicationReference = false;
    private Boolean isUtilityPatent = false;
    private int counter = 0;
    private ArrayList<DocNumberAndAbstract> buffer = new ArrayList();

    @Override
    public void startElement(String namespaceURI,
                             String localName,
                             String qName,
                             Attributes atts)
            throws SAXException {
        if(qName.equals("b")) { return; }
        currentBuffer = new StringBuffer();
        switch (qName) {
            case "publication-reference":
                isInPublicationReference = true;
                break;
            case "application-reference":
                isUtilityPatent = atts.getValue("appl-type").equalsIgnoreCase("utility");
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String currentValue = currentBuffer.toString().replaceAll("\\s+", " ");
        switch (qName) {
            case "publication-reference":
                isInPublicationReference = false;
                break;
            case "abstract":
                inventionAbstract = currentValue;
                break;
            case "us-patent-grant":
                if (isUtilityPatent) {
                    buffer.add(new DocNumberAndAbstract(docNumber, inventionAbstract));
                    // System.out.println(docNumber + ": " + inventionAbstract);
                    counter++;
                }
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

    @Override
    public void endDocument() {
        System.out.println(counter);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
    }

    public Stream<DocNumberAndAbstract> getBuffer() {
        return buffer.stream();
    }

}
