package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import searchengine.config.JsoupConnect;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.IndexingServiceImpl;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

public class SiteMap extends RecursiveAction {

    private String link = "";
    private SiteTable site;
    private TreeSet<String> links = new TreeSet<>();
    private int codeResponse;
    private String htmlCode = "";
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LuceneMorphology luceneMorphology;
    private final JsoupConnect jsoupConnect;
    private final String domainName;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public SiteMap(String link, SiteTable site,
                   SiteRepository siteRepository,
                   PageRepository pageRepository, JsoupConnect jsoupConnect,
                   LuceneMorphology luceneMorphology, LemmaRepository lemmaRepository,
                   IndexRepository indexRepository)
    {
        this.link = link;
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.jsoupConnect = jsoupConnect;
        this.domainName = site.getUrl();
        this.luceneMorphology = luceneMorphology;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    protected void compute() {
        try {
            if(IndexingServiceImpl.getInterruptIndexing()) {
                //Thread.currentThread().interrupt();
                //throw new Exception("Индексация остановлена пользователем");
                return;
            }
            fillUnderSites(link);
            if (!links.isEmpty()) {
                ForkJoinTask.invokeAll(createSubtask());
                if (link.equals(site.getUrl())) {
                    if(IndexingServiceImpl.getInterruptIndexing()){
                        site.setStatus(Status.FAILED);
                        site.setLastError("Индексация остановлена пользователем");
                    } else {
                        site.setStatus(Status.INDEXED);
                    }
                    siteRepository.save(site);
                }
            }
        }catch (Exception ex){
            site.setStatus(Status.FAILED);
            site.setStatusTime(new Date());
            site.setLastError(ex.getMessage());

            //siteRepository.save(site);
        }
    }

    private void fillUnderSites(String linkToSite){
        String path = "";
        if(linkToSite.equals(site.getUrl())){
            path = linkToSite;
        } else {
            path = linkToSite.replaceAll(site.getUrl(), "" );
        }
        Page page = createPage(path);
        if(!patchAdded(path, page)){
            return;
        }
        //if (pageRepository.siteIsInTheTable(path, site.getId()) != 0) {
        //    return;
        //}
        try {
            //int i = 1 / 0;
            Thread.sleep(300);
            //System.out.println(linkToSite);
            Connection.Response response = Jsoup.connect(linkToSite)
                    .userAgent(jsoupConnect.getUserAgent())
                    .referrer(jsoupConnect.getReferrer()).execute();
            codeResponse = response.statusCode();
            Document doc = response.parse();
            htmlCode = doc.outerHtml();

            //processing(path);
            page.setContent(htmlCode);
            page.setCode(codeResponse);
            site.setStatusTime(new Date());

            pageRepository.save(page);
            createLemmas(page, site, htmlCode);

            if(codeResponse == 200) {

                Elements elements = doc.select("a[href]");
                elements.forEach(link ->
                {
                    String linkFull = link.attr("abs:href");
                    String beginLink = "(\\Ahttp[s]?:\\/\\/([wW]{3}.)?)";
                    String endLink = domainName.replaceAll(beginLink + "(.+)", "$3");
                    if (linkFull.matches(beginLink + endLink + "/.+") && !linkFull.contains("/#")
                            && !linkFull.matches(".+\\.jpg"))
                    {
                        links.add(linkFull);
                    }
                });
            }
        } catch (Exception ex){
            site.setStatus(Status.FAILED);
            site.setStatusTime(new Date());
            site.setLastError(ex.getMessage());
            siteRepository.save(site);
        }

    }

    private List<SiteMap> createSubtask()
    {
        List<SiteMap> subTasks = new ArrayList<>();

        for(String underSite:links)
        {
            subTasks.add(new SiteMap(underSite, site, siteRepository, pageRepository, jsoupConnect,
                                        luceneMorphology, lemmaRepository, indexRepository));
        }
        return subTasks;
    }

    private synchronized boolean patchAdded(String path, Page page)
    {
        if (pageRepository.siteIsInTheTable(path, site.getId()) == 0) {
            pageRepository.save(page);
            return true;
        }
        return false;
        /*Page page = new Page();
        Boolean createLemma = false;
        synchronized (pageRepository) {
            if (pageRepository.siteIsInTheTable(path, site.getId()) == 0) {
                //page = new Page();
                page.setPath(path);
                page.setCode(codeResponse);
                page.setContent(htmlCode);
                page.setSite(site);

                site.setStatusTime(new Date());

                pageRepository.save(page);
                createLemma = true;
                //createLemmas(page, site, htmlCode);
            }
        }
        if (createLemma){
            createLemmas(page, site, htmlCode);
        }*/
    }

    private Page createPage(String patch){
        Page page = new Page();
        page.setPath(patch);
        page.setCode(200);
        page.setContent("htmlCode");
        page.setSite(site);

        return page;
    }

    void createLemmas(Page page, SiteTable site, String htmlCode)
    {
        ParsingLemmas parsingLemmas = new ParsingLemmas(luceneMorphology);
        String text = ParsingLemmas.delTag(htmlCode);
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

}

