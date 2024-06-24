package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.DataResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.ParsingLemmas;
import searchengine.repositories.DBConnection;
import searchengine.repositories.LemmaRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;

    @Override
    public IndexingResponse search(String query, String site) {

        ParsingLemmas parsingLemmas = new ParsingLemmas(luceneMorphology);
        HashMap<String,Integer> lemmas = parsingLemmas.splitTextIntoWords1(query);

        //Integer[] idPages = {};
        String queryIdPages = "";
        String lemmasToString = "";
        String queryIdPagesSample = "SELECT idx.page_id FROM search_engine.index_table AS idx\n" +
                "JOIN lemma AS lm on idx.lemma_id = lm.id\n" +
                "WHERE lm.lemma = '";
        String[] uniqueLemmas = lemmaRepository.getUniqueLemma(lemmas.keySet().toArray(new String[0]));
        for(String uniqueLemma: uniqueLemmas)
        {
            queryIdPages = queryIdPages == "" ? queryIdPagesSample + uniqueLemma + "'" :
                    queryIdPagesSample + uniqueLemma + "' AND idx.page_id in (" + queryIdPages + ")";
            lemmasToString = lemmasToString == "" ? "'" + uniqueLemma + "'" :
                                                lemmasToString + ",'" + uniqueLemma + "'";

        }
        String selectionBySite = (site == null ? "" : "siteID.url = \"" + site + "\" and ");
        queryIdPages = "with ts as (select idx.page_id as page1, sum(idx.rank_column) as sum_rank, siteID.url as url\n" +
                "FROM search_engine.index_table AS idx\n" +
                "JOIN lemma AS lm on idx.lemma_id = lm.id\n" +
                "left join search_engine.site as siteID on lm.site_id = siteID.id\n" +
                "WHERE " + selectionBySite + "lm.lemma in ("+ lemmasToString +") AND idx.page_id in (" + queryIdPages + ") group by page1)\n" +
                "select ts1.page1, (ts1.sum_rank / ts2.max_rank) as rel, page.content, page.path, ts1.url from ts as ts1 \n" +
                "left join search_engine.page as page on ts1.page1 = page.id\n" +
                "join (select max(ts.sum_rank) as max_rank from ts) as ts2;";
        ArrayList<DataResponse> res = new ArrayList<>();
        try {
            res = DBConnection.getIdPages(queryIdPages);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DataResponse[] dataResponses = new DataResponse[res.size()];
        int i = 0;
        for (DataResponse response : res){
            String content = response.getSnippet();
            response.setTitle(getTitle(content));
            response.setSnippet(createSnipped(content, uniqueLemmas, parsingLemmas));
            //response.setSite(site);
            //response.setSiteName(site);

            dataResponses[i] = response;
            i++;
        }

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        response.setData(dataResponses);
        response.setCount(dataResponses.length);
        return response;
    }

    String createSnipped(String content, String[] lemmas, ParsingLemmas parsingLemmas){
        String contentWithoutTag = ParsingLemmas.delTag(content);
        return parsingLemmas.getSniped(contentWithoutTag,lemmas);
    }

    String getTitle(String content){
        String regex = "(?<=(<title>)).*?(?=(<\\/title>))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            return content.substring(start, end);
        }
        return "";
    }
}
