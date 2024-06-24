package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexModel;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM search_engine.index_table where page_id IN" +
            "(SELECT id FROM search_engine.page where path = :path)", nativeQuery = true)
    void deleteByPath(String path);
}
