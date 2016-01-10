package SearchEngine.Import;

import SearchEngine.PatentDocument;
import org.codehaus.staxmate.in.SMInputCursor;

import javax.xml.stream.XMLStreamException;
import java.util.Optional;
import java.util.regex.Pattern;


class XmlSinglePatentReader {

    private boolean isUtilityPatent = false;
    private String inventionAbstract = null;
    private String inventionTitle = null;
    private String inventionDescription = null;
    private String inventionClaims = null;
    private int docId = 0;

    private static Pattern whitespacePattern = Pattern.compile("\\s+");

    private String cleanText(String text) {
        return whitespacePattern.matcher(text).replaceAll(" ");
    }

    private void parseBibliographicData(SMInputCursor cursor) throws XMLStreamException {
        while (cursor.getNext() != null) {
            switch (cursor.getLocalName()) {
                case "publication-reference":
                    docId = Integer.parseInt(
                            cursor
                            .childElementCursor("document-id").advance()
                            .childElementCursor("doc-number").advance()
                            .collectDescendantText());
                    break;
                case "application-reference":
                    isUtilityPatent = cursor.getAttrValue("appl-type").equals("utility");
                    break;
                case "invention-title":
                    inventionTitle = cleanText(cursor.collectDescendantText());
                    break;
            }
        }
    }

    private Optional<PatentDocument> parsePatentDocument(SMInputCursor cursor) throws XMLStreamException {
        while (cursor.getNext() != null) {
            switch (cursor.getLocalName()) {
                case "us-bibliographic-data-grant":
                    parseBibliographicData(cursor.childElementCursor());
                    break;
                case "abstract":
                    inventionAbstract = cleanText(cursor.collectDescendantText());
                    break;
                case "description":
                    inventionDescription = cleanText(cursor.collectDescendantText());
                    break;
                case "claims":
                    inventionClaims = cleanText(cursor.collectDescendantText());
                    break;
            }
        }
        if (isUtilityPatent) {
            return Optional.of(new PatentDocument(
                    docId, inventionTitle, inventionAbstract, inventionDescription, inventionClaims));
        } else {
            return Optional.empty();
        }
    }


    public static Optional<PatentDocument> parseSinglePatentDocument(SMInputCursor cursor) throws XMLStreamException {
        return new XmlSinglePatentReader().parsePatentDocument(cursor);
    }

}