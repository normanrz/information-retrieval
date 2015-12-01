package SearchEngine.DocumentIndex;

import SearchEngine.PatentDocument;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by norman on 30.11.15.
 */


public class XmlPatentReader {

    private static SMInputCursor createCursor(InputStream inputStream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        SMInputFactory smInputFactory = new SMInputFactory(XMLInputFactory.newInstance());
        return smInputFactory.rootElementCursor(inputStream);
    }

    public static Optional<PatentDocument> readSingle(InputStream inputStream) throws XMLStreamException {
        SMInputCursor reader = createCursor(inputStream);

        if (reader.getNext() != null) {
            if (reader.getLocalName().equals("us-patent-grant")) {
                return XmlSinglePatentReader.parseSinglePatentDocument(reader.childElementCursor());
            }
        }
        return Optional.empty();
    }


    public static List<PatentDocument> readMultiple(InputStream inputStream) throws XMLStreamException {
        SMInputCursor reader = createCursor(inputStream).advance().childElementCursor();

        List<PatentDocument> list = new ArrayList<>();
        while (reader.getNext() != null) {
            if (reader.getLocalName().equals("us-patent-grant")) {
                XmlSinglePatentReader.parseSinglePatentDocument(reader.childElementCursor())
                        .ifPresent(list::add);
            }
        }
        return list;
    }

}
