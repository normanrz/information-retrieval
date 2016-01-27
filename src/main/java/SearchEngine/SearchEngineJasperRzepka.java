package SearchEngine;

import SearchEngine.DocumentIndex.XmlDocumentIndex;
import SearchEngine.Import.PatentDocumentImporter;
import SearchEngine.InvertedIndex.InvertedIndexMerger;
import SearchEngine.InvertedIndex.disk.DiskInvertedIndex;
import SearchEngine.InvertedIndex.memory.MemoryInvertedIndex;
import SearchEngine.LinkIndex.LinkIndex;
import SearchEngine.Query.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author: JasperRzepka
 * @dataset: US patent grants : ipg files from http://www.google.com/googlebooks/uspto-patents-grants-text.html
 * @course: Information Retrieval and Web Search, Hasso-Plattner Institut, 2015
 * <p>
 * This is your file! implement your search engine here!
 * <p>
 * Describe your search engine briefly:
 * - multi-threaded?
 * - stemming?
 * - stopword removal?
 * - index algorithm?
 * - etc.
 * <p>
 * Keep in mind to include your implementation decisions also in the pdf file of each assignment
 */


public class SearchEngineJasperRzepka implements AutoCloseable {

    private static final String invertedIndexFileName = "inverted.index";
    private static final String documentIndexFileName = "document.index";
    private static final String linkIndexFileName = "link.index";

    protected DiskInvertedIndex index;
    protected XmlDocumentIndex docIndex;
    protected LinkIndex linkIndex;


    public static void indexSingle(
            String dataDirectory, File inputFile, File outputInvertedIndexFile, File outputDocumentIndexFile, File outputLinkIndexFile) {
        XmlDocumentIndex documentIndex = new XmlDocumentIndex(dataDirectory);
        LinkIndex linkIndex = new LinkIndex();

        System.out.println(inputFile.getName());

        MemoryInvertedIndex localIndex = new MemoryInvertedIndex();

        PatentDocumentImporter.importPatentDocuments(inputFile, localIndex, documentIndex, linkIndex);

        System.out.println(outputInvertedIndexFile.getName());
        System.out.println(outputDocumentIndexFile.getName());
        System.out.println(outputLinkIndexFile.getName());
        localIndex.printStats();
        try {
            localIndex.save(outputInvertedIndexFile);
            documentIndex.save(outputDocumentIndexFile);
            linkIndex.save(outputLinkIndexFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void index(String dataDirectory, String outputDirectory) {

        XmlDocumentIndex documentIndex = new XmlDocumentIndex(dataDirectory);
        LinkIndex linkIndex = new LinkIndex();
        AtomicInteger subIndexCounter = new AtomicInteger(0);
        List<File> subIndexFiles = new ArrayList<>();

        new File(outputDirectory).mkdirs();

        File dir = new File(dataDirectory);
        Stream.of(dir.listFiles()).parallel()
                .filter(file -> file.getName().endsWith((".xml")))
                .forEach(file -> {
                    System.out.println(file.getName());

                    MemoryInvertedIndex localIndex = new MemoryInvertedIndex();

                    PatentDocumentImporter.importPatentDocuments(file, localIndex, documentIndex, linkIndex);

                    File indexFile = new File(outputDirectory,
                            String.format("%s.%02d", invertedIndexFileName, subIndexCounter.getAndIncrement()));
                    subIndexFiles.add(indexFile);

                    System.out.println(indexFile.getName());
                    localIndex.printStats();
                    try {
                        localIndex.save(indexFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        try {
            linkIndex.save(new File(outputDirectory, linkIndexFileName));
            documentIndex.save(new File(outputDirectory, documentIndexFileName));
            InvertedIndexMerger.merge(subIndexFiles, new File(outputDirectory, invertedIndexFileName));
            subIndexFiles.forEach(File::delete);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void loadIndex(String indexDirectory, String dataDirectory) {
        try {
            linkIndex = LinkIndex.load(new File(indexDirectory, linkIndexFileName));
            index = new DiskInvertedIndex(new File(indexDirectory, invertedIndexFileName));
            index.printStats();
            docIndex = XmlDocumentIndex.load(dataDirectory, new File(indexDirectory, documentIndexFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private SnippetSearchResult createSnippetSearchResult(
            SearchResult result, List<String> queryTokens) {

        PatentDocument doc = docIndex.getPatentDocument(result.getDocId()).get();
        return new SnippetSearchResult(
                result, doc,
                SnippetGenerator.getSnippets(doc, queryTokens));
    }

    private SnippetSearchResult createSnippetFromBodySearchResult(
            SearchResult result, List<String> queryTokens) {

        PatentDocument doc = docIndex.getPatentDocument(result.getDocId()).get();
        return new SnippetSearchResult(
                result, doc,
                SnippetGenerator.getSnippetsFromBody(doc, queryTokens));
    }

    private Map<String, Double> pseudoRelevanceModelWithSnippets(
            Stream<SearchResult> rankResults, int prf, List<String> queryTokens) {

        Ranker ranker = new Ranker(index, docIndex);

        // Generate snippets
        List<SnippetSearchResult> prfDocumentSnippetsResults = rankResults
                .limit(prf)
                .map(result -> createSnippetSearchResult(result, queryTokens))
                .collect(Collectors.toList());

        // Pseudo relevance feedback model with snippets
        return ranker.pseudoRelevanceModel(queryTokens, prfDocumentSnippetsResults);
    }

    private Map<String, Double> pseudoRelevanceModelWithDocuments(
            Stream<SearchResult> rankResults, int prf, List<String> queryTokens) {

        Ranker ranker = new Ranker(index, docIndex);

        // Pseudo relevance feedback model with whole documents
        int[] topRankedDocIds = rankResults.limit(prf).mapToInt(SearchResult::getDocId).toArray();
        return ranker.pseudoRelevanceModel(queryTokens, topRankedDocIds);
    }

    public Stream<SnippetSearchResult> search(String query, int prf) {

        // Set up
        Ranker ranker = new Ranker(index, docIndex);

        // Search
        SearchResultSet searchResultSet = Searcher.search(query, index, false);
        List<String> queryTokens = searchResultSet.getQueryTokens();

        // Rank first-pass
        Stream<SearchResult> rankResults = ranker.rank(searchResultSet);

        if (prf == 0) {
            return rankResults
                    .map(result -> createSnippetFromBodySearchResult(result, queryTokens));
        } else {

            Map<String, Double> relevanceModel = pseudoRelevanceModelWithSnippets(rankResults, prf, queryTokens);
//            Map<String, Double> relevanceModel = pseudoRelevanceModelWithDocuments(rankResults, prf, queryTokens);

            List<String> newQueryTokens = Ranker.expandQueryFromRelevanceModel(relevanceModel, queryTokens);
            String newQuery = String.join(" OR ", newQueryTokens);

            // Search and rank again
            return ranker.rankWithRelevanceModel(Searcher.search(newQuery, index, false).getDocIds(), relevanceModel).stream()
                    .map(result -> createSnippetFromBodySearchResult(result, newQueryTokens));
        }

    }

    public List<String> search(String query, int topK, int prf) {

//        List<Integer> googleIds = new WebFile().getGoogleRanking(query).stream()
//                .limit(topK)
//                .collect(Collectors.toList());

        List<SnippetSearchResult> results = search(query, prf)
                .limit(topK)
                .collect(Collectors.toList());

//        System.out.println(computeNDCG(googleIds, results, topK));

        return results.stream()
                .map(result -> result.toString())
                .collect(Collectors.toList());

    }

    double getGain(List<Integer> goldRanking, int docId) {
        if (goldRanking.contains(docId)) {
            return 1 + Math.floor(10 * Math.pow(0.5, 0.1 * (goldRanking.indexOf(docId) + 1)));
        } else {
            return 0;
        }
    }

    public double computeNDCG(List<Integer> goldRanking, List<SnippetSearchResult> results, int p) {
        AtomicInteger i = new AtomicInteger(1);
        double dcg = results.stream()
                .limit(p)
                .mapToDouble(result -> {
                    double value = getGain(goldRanking, result.getDocId()) / ((i.get() == 1) ? 1 : Math.log(i.get()));
                    i.incrementAndGet();
                    return value;
                })
                .sum();

        AtomicInteger j = new AtomicInteger(1);
        double idcg = goldRanking.stream()
                .limit(p)
                .mapToDouble(docId -> getGain(goldRanking, docId))
                .map(gain -> {
                    double value = gain / ((j.get() == 1) ? 1 : Math.log(j.get()));
                    j.incrementAndGet();
                    return value;
                })
                .sum();

        if (idcg == 0) {
            return 0;
        } else {
            return dcg / idcg;
        }
    }

    public void close() throws IOException {
        index.close();
    }

}
