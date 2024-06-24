package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConnect;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.ParsingLemmas;
import searchengine.dto.indexing.SiteList;
import searchengine.dto.indexing.SiteMap;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sites;
    private final JsoupConnect jsoupConnect;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LuceneMorphology luceneMorphology;
    private static String domainName = "(\\Ahttp[s]?:\\/\\/([wW]{3}\\.)?)(.[^\\/]+)";
    private ForkJoinPool forkJoinPool;
    private static Boolean interruptIndexing = false;

    @Override
    public IndexingResponse startIndexing() {
        interruptIndexing = false;
        List<Site> sitesList = sites.getSites();
        IndexingResponse response = new IndexingResponse();

        String[] siteArray = new String[sitesList.size()];
        for (int i = 0; i < sitesList.size(); i++)
        {
            siteArray[i] = sitesList.get(i).getUrl();
        }
        /*if(siteRepository.isIndexing(siteArray) == 1) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        }*/

        forkJoinPool = ForkJoinPool.commonPool();
        SiteList siteList = new SiteList(sitesList, jsoupConnect, siteRepository, pageRepository, luceneMorphology,
                                                lemmaRepository, indexRepository);
        //forkJoinPool.invoke(siteList);
        siteList.fork();

        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (siteRepository.isStartIndexing()>0){
            interruptIndexing = true;
            response.setResult(true);
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }

    @Override
    public IndexingResponse updateIndex(String url) {
        IndexingResponse indexingResponse = new IndexingResponse();
        String urlDomen = url.replaceAll("(\\Ahttp[s]?:\\/\\/([wW]{3}\\.)?)(.[^\\/]+)(\\/.+)","$3");
        List<Site> siteList = sites.getSites();
        Optional<Site> optionalSite = siteList.stream().
                filter(s -> s.getUrl().replaceAll("(\\Ahttp[s]?:\\/\\/([wW]{3}\\.)?)(.[^\\/]+)(\\/)?","$3").
                        equals(urlDomen)).findFirst();
        if (optionalSite.isEmpty()) {
            indexingResponse.setError("Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле\n");
            indexingResponse.setResult(false);
            return indexingResponse;
        }
        String path = url.replaceAll(domainName, "");
        indexRepository.deleteByPath(path);
        lemmaRepository.deleteLemma();
        pageRepository.deleteByPath(path);
        int codeResponse = 0;
        String htmlCode = "";
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(jsoupConnect.getUserAgent())
                    .referrer(jsoupConnect.getReferrer()).execute();
            codeResponse = response.statusCode();
            Document doc = response.parse();
            htmlCode = doc.outerHtml();

            if (codeResponse == 200) {
                int idSite = siteRepository.getId(optionalSite.get().getUrl());
                Optional<SiteTable> siteTable = siteRepository.findById(idSite);
                SiteTable site = new SiteTable();
                if(siteTable.isEmpty())
                {
                    site.setStatus(Status.INDEXING);
                    site.setStatusTime(new Date());
                    site.setUrl(optionalSite.get().getUrl());
                    site.setName(optionalSite.get().getName());

                    siteRepository.save(site);

                } else {
                    site = siteTable.get();
                }
                Page page = new Page();
                page.setPath(path);
                page.setCode(codeResponse);
                page.setContent(htmlCode);
                page.setSite(site);

                pageRepository.save(page);

                createLemmas(page, site, htmlCode);

            }
        } catch (Exception ex){
            indexingResponse.setError(ex.getMessage());
            indexingResponse.setResult(false);
            return indexingResponse;
        }
        indexingResponse.setResult(true);
        return indexingResponse;
    }

    void deleteAllByUrl(String url)
    {
        pageRepository.deleteByUrl(url);
        siteRepository.deleteByUrl(url);
    }

    void createLemmas(Page page, SiteTable site, String htmlCode)
    {
        ParsingLemmas parsingLemmas = new ParsingLemmas(luceneMorphology);
        String text = parsingLemmas.delTag(htmlCode);
        //HashMap<String, Integer> lemmas =
        parsingLemmas.splitTextIntoWords(text, site.getId(), page.getId());

        /*lemmas.forEach((lemma, rank) -> {
            int idLemma = lemmaRepository.getId(lemma, site.getId());
            Optional<Lemma> optionalLemma = lemmaRepository.findById(idLemma);
            if (optionalLemma.isEmpty()){
                Lemma lemmaTable = new Lemma();
                addEntryLemma(lemmaTable, page, site, 1, lemma, rank);
            } else {
                Lemma lemmaTable = optionalLemma.get();
                addEntryLemma(lemmaTable, page, site, lemmaTable.getFrequency() + 1, lemma, rank);
            }
        });*/
    }

    void addEntryLemma(Lemma lemmaTable, Page page, SiteTable site, int frequency, String lemma,  int rank)
    {
        lemmaTable.setLemma(lemma);
        lemmaTable.setSite(site);
        lemmaTable.setFrequency(frequency);
        lemmaRepository.save(lemmaTable);

        IndexModel indexModel = new IndexModel();
        indexModel.setLemma(lemmaTable);
        indexModel.setPage(page);
        indexModel.setRank(Double.valueOf(rank));
        indexRepository.save(indexModel);
    }

    public static Boolean getInterruptIndexing(){
        return interruptIndexing;
    }
}
