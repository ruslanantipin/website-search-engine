package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {

    @Query(value = "select count(*) FROM search_engine.page where path = :path and site_id = :siteId", nativeQuery = true)
    int siteIsInTheTable(String path, int siteId);

    @Modifying
    @Transactional
    @Query(value = "delete FROM search_engine.page where site_id in (SELECT site.id FROM search_engine.site as site where site.url = :url) limit 10000", nativeQuery = true)
    void deleteByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "delete FROM search_engine.page where path = :path", nativeQuery = true)
    void deleteByPath(String path);
}
