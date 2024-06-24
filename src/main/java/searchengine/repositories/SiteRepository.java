package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteTable;

@Repository
public interface SiteRepository extends JpaRepository<SiteTable, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM search_engine.site WHERE url = :url", nativeQuery = true)
    void deleteByUrl(String url);

    @Query(value = "SELECT count(*) FROM search_engine.site where url in (:listUrl) and status = 'INDEXING';", nativeQuery = true)
    int isIndexing(String[] listUrl);

    @Query(value = "SELECT if(id is null, count(*), id) FROM search_engine.site where url = :url1", nativeQuery = true)
    int getId(String url1);

    @Query(value = "SELECT count(*) FROM search_engine.site where status = 'INDEXING';", nativeQuery = true)
    int isStartIndexing();
}
