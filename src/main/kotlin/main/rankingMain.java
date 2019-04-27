package main;

import org.apache.commons.lang3.ObjectUtils;
import util.CSVParser;
import util.RocksDB;

import java.util.*;
import java.util.stream.Collectors;

public class rankingMain {


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

    private final String WORD_DB_NAME = "$BASE_URL/rockWord";
    private final String URL_WORDS_DB_NAME = "$BASE_URL/rockUrlWords";

    //Function to get word ids given an array of strings
    private Vector<String> findWordId(String[] inputWords){
        // word -> wordID
        RocksDB wordDB = new RocksDB(WORD_DB_NAME);
        Vector<String> result = new Vector<>(0);
        for (String inputWord : inputWords) {
            if (wordDB.getKey(inputWord) == null){
                //Query term not found in any docs; for now just ignore
                break;
            } else {
                result.add(wordDB.getKey(inputWord));
            }
        }
        wordDB.close();
//        Collections.sort(result);

        return result;      //Returns a list of wordId that corresponds to query
    }

    //Function to get list of terms from a doc
    private List<String> getTermsFromDoc(String urlKey){
        // urlID -> list(wordID)
        RocksDB urlWordsDB = new RocksDB(URL_WORDS_DB_NAME);

        List<String> result = CSVParser.INSTANCE.parseFrom(urlWordsDB.get(urlKey));

        urlWordsDB.close();
        return result;
    }

    //Function to find docs that contains at least one of the query terms
    private Vector<String> findDocs(Vector<String> queryTerms){
        // urlID -> list(wordID)
        RocksDB urlWordsDB = new RocksDB(URL_WORDS_DB_NAME);

        List<String> urlKeys = urlWordsDB.getAllKeys();
        Vector<String> result = new Vector<>(0);

        for (String urlKey : urlKeys) {
            for (String queryTerm : queryTerms) {
                //For each link, check if any of the query term id is present
                if (getTermsFromDoc(urlKey).indexOf(queryTerm) != -1) {
                    //Add to doc id to result if query term is in the doc
                    result.add(urlKey);
                    break;
                }
            }
        }

        urlWordsDB.close();

        return result;

    }

    //Given termId and urlId count number of terms in doc
    private int countTermInDoc (String wordIdIn, String urlIdIn){
        List<String> wordIdsInDoc = getTermsFromDoc(urlIdIn);

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
    private double cosSim(String urlId, Vector<String> queryTermIds){
        //dikQk stores Sum of all query terms (dik * qk) [Nominator of cosSim]
        double dikQk = 0.0;

        //dik stores sum of all term frequency in doc squared
        double dik = 0.0;

        //qk stores sum of all query term squared
        double qk = 0.0;

        for (String queryTermId : queryTermIds){
            dikQk += countTermInDoc(queryTermId, urlId) * getQueryWeight(queryTermId);
            dik += Math.pow(countTermInDoc(queryTermId, urlId), 2);
            qk += Math.pow(getQueryWeight(queryTermId), 2);
        }
        return dikQk / Math.sqrt(dik * qk);

    }

    /*
     * Given an array of queryTerms, return a list of DocIds containing
     * at least one of the query terms, sorted by descending order of cosSim values
     */

    protected List<String> rankDocs(String[] queryTerms) {

        Vector<String> queryTermIds = findWordId(queryTerms);
        Vector<String> docsWithQuery = findDocs(queryTermIds);
        Map<String, Double> docCosSim = new HashMap<>();

        for (String docId : docsWithQuery) {
            docCosSim.put(docId, cosSim(docId, queryTermIds));
        }

        //Sort by values (cosine similarities)
        Map<String, Double> sortedCosSim = docCosSim
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (e1, e2) -> e2, LinkedHashMap::new));

        return new ArrayList<>(sortedCosSim.keySet());
    }


}
