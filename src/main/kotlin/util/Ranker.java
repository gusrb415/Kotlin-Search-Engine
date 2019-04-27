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
    private List<String> getTermsFromDoc(String urlKey, RocksDB db){
        // urlID -> list(wordID)
        return CSVParser.INSTANCE.parseFrom(Objects.requireNonNull(db.get(urlKey)));
    }

    //Function to find docs that contains at least one of the query terms
    private Vector<String> findDocs(Vector<String> queryTerms, RocksDB urlWordsDB){
        // urlID -> list(wordID)

        List<String> urlKeys = urlWordsDB.getAllKeys();
        Vector<String> result = new Vector<>(0);

        for (String urlKey : urlKeys) {
            for (String queryTerm : queryTerms) {
                //For each link, check if any of the query term id is present
                if (getTermsFromDoc(urlKey, urlWordsDB).indexOf(queryTerm) != -1) {
                    //Add to doc id to result if query term is in the doc
                    result.add(urlKey);
                    break;
                }
            }
        }

        return result;

    }

    //Given termId and urlId count number of terms in doc
    private int countTermInDoc (String wordIdIn, String urlIdIn, RocksDB urlWordsDB){
        List<String> wordIdsInDoc = getTermsFromDoc(urlIdIn, urlWordsDB);
        int count = 0;

        for (String wordId : wordIdsInDoc){
            if (wordId.equals(wordIdIn))
                count++;
        }

        return count;
    }

    //Returns query weight of term given a term id
    //For now, just return 1.0 every time.
    private double getQueryWeight(String termId){
        return 1.0;
    }

    //Given urlId and queryTermId calculate cosine similarity
    private double cosSim(String urlId, Vector<String> queryTermIds, RocksDB urlWordsDB){
        //dikQk stores Sum of all query terms (dik * qk) [Nominator of cosSim]
        double dikQk = 0.0;

        //dik stores sum of all term frequency in doc squared
        double dik = 0.0;

        //qk stores sum of all query term squared
        double qk = 0.0;

        for (String queryTermId : queryTermIds){
            dikQk += countTermInDoc(queryTermId, urlId, urlWordsDB) * getQueryWeight(queryTermId);
            dik += Math.pow(countTermInDoc(queryTermId, urlId, urlWordsDB), 2);
            qk += Math.pow(getQueryWeight(queryTermId), 2);
        }
        return dikQk / Math.sqrt(dik * qk);

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
    public Map<String, Double> rankDocs(String[] queryTerms, RocksDB urlWordsDB) {
        Vector<String> queryTermIds = findWordId(queryTerms);
        System.out.println(queryTermIds);
        Vector<String> docsWithQuery = findDocs(queryTermIds, urlWordsDB);
        System.out.println(docsWithQuery);

        Map<String, Double> docCosSim = new HashMap<>();

        for (String docId : docsWithQuery) {
            docCosSim.put(docId, cosSim(docId, queryTermIds, urlWordsDB));
        }

        //Sort by values (cosine similarities)
        Map<String, Double> sortedCosSim = docCosSim
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e2, LinkedHashMap::new));

        return sortedCosSim;

//        return new ArrayList<>(sortedCosSim.keySet());    //Return only urlId ranked by score
    }


}
