package SearchEngine.Test;

import SearchEngine.SearchEngineJasperRzepka;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by norman on 01.12.15.
 */
public class SearchTest {

    SearchEngineJasperRzepka engine = new SearchEngineJasperRzepka();

    @Before
    public void beforeAll() {
        engine.loadIndex("index", "data");
    }

    @Test
    public void testSpellingCorrection() {
        String[] goodQueries = {"commom", "incluce", "streem"};
        String[] badQueries = {"asdjlis", "kontrol"};

        for (String query : goodQueries) {
            Assert.assertTrue(engine.search(query, 0).count() > 0);
        }

        for (String query : badQueries) {
            Assert.assertTrue(engine.search(query, 0).count() == 0);
        }
    }

    @Test
    public void testPseudoRelevanceFeedback() {
        String[] queries = {"digital", "rootkits", "network OR access"};

        for (String query : queries) {
            Assert.assertTrue(query, engine.search(query, 2).count() > 10);
        }
    }

    @Test
    public void testPhraseQueries() {
        String[] queries = {"mobile devices"};
        for (String query : queries) {
            Assert.assertTrue(query, engine.search(query, 0).count() > 0);
        }
    }

    @Test
    public void testPrefixQueries() {
        String[] queries = {"mob*", "prov*"};
        for (String query : queries) {
            Assert.assertTrue(query, engine.search(query, 0).count() > 0);
        }
    }

    @Test
    public void testSnippets() {
        String[] queries = {
                "digital", "rootkits", "network OR access",
                "access OR control", "computers", "data OR processing", "web OR servers",
                "vulnerability OR information", "computer OR readable OR media"};

        for (String query : queries) {
            engine.search(query, 0)
                    .forEach(searchResult ->
                            Assert.assertTrue(query + " " + searchResult.getDocId(),
                                    searchResult.getSnippets().size() > 0));
        }
    }

}
