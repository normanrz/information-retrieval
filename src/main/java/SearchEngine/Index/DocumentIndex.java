package SearchEngine.Index;

import SearchEngine.PatentDocument;
import org.fusesource.lmdbjni.Database;
import org.fusesource.lmdbjni.Env;

import static org.fusesource.lmdbjni.Constants.bytes;
import static org.fusesource.lmdbjni.Constants.string;

/**
 * Created by norman on 16.11.15.
 */
public class DocumentIndex implements AutoCloseable {

    private final Env env;
    private final Database db;

    public DocumentIndex(String directory) {
        this.env = new Env(directory);
        this.db = this.env.openDatabase();
    }

    public void storePatentDocument(PatentDocument doc) {
        db.put(bytes(String.format("%08d:title", doc.docId)), bytes(doc.title));
        db.put(bytes(String.format("%08d:abstract", doc.docId)), bytes(doc.abstractText));
    }

    public String getPatentDocumentTitle(int docId) {
        return string(db.get(bytes(String.format("%08d:title", docId))));
    }

    @Override
    public void close() {
        db.close();
        env.close();
    }
}
