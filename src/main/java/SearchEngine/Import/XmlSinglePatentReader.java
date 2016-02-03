package SearchEngine.Import;

import SearchEngine.PatentDocument;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;
import org.codehaus.staxmate.in.SMInputCursor;

import javax.xml.stream.XMLStreamException;
import java.util.Optional;
import java.util.regex.Pattern;


class XmlSinglePatentReader {

    private static final Pattern whitespacePattern = Pattern.compile("\\s+");
    private static final Pattern docIdPattern = Pattern.compile("^\\d{3,8}$");
    private static final int minDocId = 7861317;
    private static final int maxDocId = 8984661;

    private boolean isUtilityPatent = false;
    private String inventionAbstract = null;
    private String inventionTitle = null;
    private String inventionDescription = null;
    private String inventionClaims = null;
    private IntList inventionCitations = new ArrayIntList();
    private int docId = 0;

    public static Optional<PatentDocument> parseSinglePatentDocument(SMInputCursor cursor) throws XMLStreamException {
        return new XmlSinglePatentReader().parsePatentDocument(cursor);
    }

    private XmlSinglePatentReader() {
    }

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
                case "us-references-cited":
                case "references-cited":
                    parseCitations(cursor.childElementCursor());
                    break;
            }
        }
    }

    private void parseCitations(SMInputCursor cursor) throws XMLStreamException {
        while (cursor.getNext() != null) {
            switch (cursor.getLocalName()) {
                case "us-citation":
                case "citation":
                    parseCitation(cursor.childElementCursor());
                    break;
            }
        }
    }

    private void parseCitation(SMInputCursor cursor) throws XMLStreamException {
        while (cursor.getNext() != null) {
            switch (cursor.getLocalName()) {
                case "patcit":
                    SMInputCursor documentIdCursor = cursor
                            .childElementCursor("document-id").advance().childElementCursor();

                    boolean isUSPatent = false;
                    while (documentIdCursor.getNext() != null) {
                        switch (documentIdCursor.getLocalName()) {
                            case "country":
                                isUSPatent = documentIdCursor.collectDescendantText().equalsIgnoreCase("US");
                                break;
                            case "doc-number":
                                if (isUSPatent) {
                                    String docIdString = documentIdCursor.collectDescendantText();
                                    if (docIdPattern.matcher(docIdString).matches()) {
                                        int docId = Integer.parseInt(docIdString);
                                        if (minDocId <= docId && docId <= maxDocId) {
                                            inventionCitations.add(docId);
                                        }
                                    }
                                }
                                break;
                        }
                    }

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
                    docId, inventionTitle, inventionAbstract, inventionDescription, inventionClaims, inventionCitations));
        } else {
            return Optional.empty();
        }
    }

}