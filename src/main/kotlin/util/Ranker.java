package util;

import main.SpiderMain;
import java.util.*;
import java.util.stream.Collectors;

public class Ranker {
    //Calculate Cosine Similarity of Each Doc

    /*
     *  Weight of query term can be determined by tf * idf
     *
     *  Given:
     *  A query (q) with k query terms  (Let qk denote the weight of term k in the query)
     *  Doc i (di) with k terms         (Let dik denote the frequency of term k in doc i)
     *
     *  Variables needed:
     *  Frequency of query term in the doc  (dik)
     *  Weight of query term                (qk)
     *  Formula:
     *  Sum of all query terms (dik * qk) / sqrt ( (Sum of all query terms in doc (dik))^2 * (Sum of all query terms (qk))^2 )
     */

//    private final String WORD_DB_NAME = "$BASE_URL/rockWord";
//    private final String URL_WORDS_DB_NAME = "$BASE_URL/rockUrlWords";

    //Function to get word ids given an array of strings
    private Vector<String> findWordId(String[] inputWords){
        // word -> wordID
        RocksDB wordDB = new RocksDB(SpiderMain.WORD_DB_NAME);
        Vector<String> result = new Vector<>(0);
        for (String inputWord : inputWords) {
            if (wordDB.get(inputWord) == null){
                //Query term not found in any docs; for now just ignore
                break;
            } else {
                result.add(wordDB.get(inputWord));
            }
        }
        wordDB.close();
//        Collections.sort(result);

        return result;      //Returns a list of wordId that corresponds to query
    }

    //Function to get list of terms from a doc
    private List<String> getTermsFromDoc(String urlKey, RocksDB urlWordsDB){

        // urlID -> list(wordID)
        return CSVParser.INSTANCE.parseFrom(Objects.requireNonNull(urlWordsDB.get(urlKey)));
    }

    //Function to find docs that contains at least one of the query terms
    private Vector<String> findDocs(Vector<String> queryTermIds, RocksDB urlWordsDB){
        // urlID -> list(wordID)

        List<String> urlKeys = urlWordsDB.getAllKeys();
        Vector<String> result = new Vector<>(0);

        for (String urlKey : urlKeys) {
            for (String queryTermId : queryTermIds) {
                //For each link, check if any of the query term id is present
                if (getTermsFromDoc(urlKey, urlWordsDB).indexOf(queryTermId) != -1) {
                    //Add to doc id to result if query term is in the doc
                    result.add(urlKey);
                    break;
                }
            }
        }

        return result;

    }

    //Find docs given a single query term
    private Vector<String> findDocs(String queryTermId, RocksDB urlWordsDB){
        // urlID -> list(wordID)

        List<String> urlKeys = urlWordsDB.getAllKeys();
        Vector<String> result = new Vector<>(0);

        for (String urlKey : urlKeys) {
            //For each link, check if any of the query term id is present
            if (getTermsFromDoc(urlKey, urlWordsDB).indexOf(queryTermId) != -1) {
                //Add to doc id to result if query term is in the doc
                result.add(urlKey);
            }
        }

        return result;

    }

    //Given termId and urlId count number of terms in doc
    private int countTermInDoc (String wordIdIn, String urlIdIn, RocksDB urlWordsDB){
        List<String> wordIdsInDoc = getTermsFromDoc(urlIdIn, urlWordsDB);
        int count = 0;

        for (String wordId : wordIdsInDoc){
            if (wordId.equalsIgnoreCase(wordIdIn))
                count++;
        }

        return count;
    }

    //Returns query weight of term given a term id
    //For now, just return 1.0 every time.
    private double getQueryWeight(String termId){
        return 1.0;
    }

    private int getDocCount(RocksDB urlDB){
        return urlDB.getAllKeys().size();
    }

    private double idf(String termId, RocksDB urlDB, RocksDB urlWordsDB){
        return Math.log((getDocCount(urlDB) * 1.0 / findDocs(termId, urlWordsDB).size()))
                / Math.log(2.0);
    }

    private int tf(String termId, String urlId, RocksDB spiderDB){

        int tf = 0;

        if (spiderDB.get(termId) == null){
            return 0;
        }

        String[] pairs = spiderDB.get(termId).split("d");
        for (String pair : pairs) {
            String[] keyValue = pair.split(" ");

            if (keyValue[0].equals(urlId)){
                tf++;
            }
        }
        System.out.println(tf);
        return tf;


//        return countTermInDoc(termId, urlId, urlWordsDB);
    }

    private double tfIdf(String termId, String urlId, RocksDB urlDB, RocksDB urlWordsDB){

        double tf = countTermInDoc(termId, urlId, urlWordsDB);
        double idf = Math.log((getDocCount(urlDB) * 1.0 / findDocs(termId, urlWordsDB).size()))
                / Math.log(2.0);

        return tf * idf;

    }


    //Given urlId and queryTermId calculate cosine similarity
    private double cosSim(String urlId, Vector<String> queryTermIds, RocksDB urlWordsDB, RocksDB urlDB, RocksDB spiderDB){

        System.out.print("Doc ID: ");
        System.out.println(urlId);

        //dikQk stores Sum of all query terms (dik * qk) [Inner product of document and query]
        double dikQk = 0.0;

        //qk stores sum of all query term squared
        double qk = 0.0;

        for (String queryTermId : queryTermIds){
            double idf = idf(queryTermId, urlDB, urlWordsDB);
            dikQk += tf(queryTermId, urlId, urlWordsDB) * idf * getQueryWeight(queryTermId);
            qk += Math.pow(getQueryWeight(queryTermId), 2.0);
        }

        if (dikQk == 0 || qk == 0){
            return 0.0;
        }

        //dik stores sum of all term frequency in doc squared
        double dik = 0.0;
        List<String> allTermIdsInDoc = getTermsFromDoc(urlId, urlWordsDB);
        List<String> allUniqueTermsInDoc = allTermIdsInDoc.stream().distinct().collect(Collectors.toList());

        System.out.println(allUniqueTermsInDoc.size());
        System.out.println(allUniqueTermsInDoc);

        for (String termId : allUniqueTermsInDoc){
            double idf = idf(termId, urlDB, urlWordsDB);

            dik += Math.pow(tf(termId, urlId, spiderDB) * idf, 2.0);
        }

        dik = Math.sqrt(dik);
        qk = Math.sqrt(qk);

        System.out.println(dikQk);
        System.out.println(dik);
        System.out.println(qk);

        return dikQk / dik * qk;

    }

    /*
     * Given an array of queryTerms, return a list of DocIds containing
     * at least one of the query terms, sorted by descending order of cosSim values
     */
//    public List<String> rankDocs(String[] queryTerms, RocksDB urlWordsDB) {

    /*
     * Given an array of queryTerms, return a map of DocIds to score.
     * Contains docs with at least one of the query terms,
     * sorted by descending order of cosSim values
     */
    public Map<String, Double> rankDocs(String[] queryTerms, RocksDB urlWordsDB, RocksDB urlDB, RocksDB spiderDB) {
        Vector<String> queryTermIds = findWordId(queryTerms);
        System.out.println(queryTermIds);
        Vector<String> docsWithQuery = findDocs(queryTermIds, urlWordsDB);
        System.out.println(docsWithQuery);

        Map<String, Double> docCosSim = new HashMap<>();

        for (String docId : docsWithQuery) {
            docCosSim.put(docId, cosSim(docId, queryTermIds, urlWordsDB, urlDB, spiderDB));
        }

        return docCosSim;
/*
        //Sort by values (cosine similarities)
        Map<String, Double> sortedCosSim = docCosSim
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e2, LinkedHashMap::new));

        return sortedCosSim;        //Return map of urlId to score, ranked by score

//        return new ArrayList<>(sortedCosSim.keySet());    //Return only array with urlId only, ranked by score
*/
    }


}
