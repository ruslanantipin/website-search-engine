package searchengine.dto.indexing;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import searchengine.config.JsoupConnect;
import searchengine.config.Site;
import searchengine.model.SiteTable;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

//@Service
@RequiredArgsConstructor
public class SiteList extends RecursiveAction {
        private final List<Site> sitesList;
        private final JsoupConnect jsoupConnect;
        private final SiteRepository siteRepository;
        private final PageRepository pageRepository;
        private final LuceneMorphology luceneMorphology;
        private final LemmaRepository lemmaRepository;
        private final IndexRepository indexRepository;

        @Override
        protected void compute() {
                List<SiteMap> subTasks = new ArrayList<>();
                for(Site site:sitesList){
                        String url = site.getUrl();

                        SiteTable siteModel = new SiteTable();
                        siteModel.setStatus(Status.INDEXING);
                        siteModel.setStatusTime(new Date());
                        siteModel.setUrl(url);
                        siteModel.setName(site.getName());

                        initialData(url, siteModel);

                        subTasks.add(new SiteMap(site.getUrl(), siteModel, siteRepository, pageRepository, jsoupConnect,
                                                        luceneMorphology, lemmaRepository, indexRepository));
                }
                ForkJoinTask.invokeAll(subTasks);
        }

        void initialData(String url, SiteTable siteTable)
        {
                try {
                        //pageRepository.deleteByUrl(url);
                        siteRepository.deleteByUrl(url);
                        //siteRepository.deleteById(1);
                        siteRepository.save(siteTable);
                }catch (Exception ex){
                        siteTable.setStatus(Status.FAILED);
                        siteTable.setStatusTime(new Date());
                        siteTable.setLastError(ex.getMessage());

                        siteRepository.save(siteTable);
                }
        }
}
