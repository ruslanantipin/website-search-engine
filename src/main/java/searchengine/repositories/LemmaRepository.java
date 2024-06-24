package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM search_engine.lemma AS del_lemma where del_lemma.id IN\n" +
            "  (SELECT null_lemma.id FROM \n" +
            "\t(SELECT lemma.id FROM search_engine.lemma AS lemma\n" +
            "\tLEFT JOIN search_engine.index_table AS idx_t\n" +
            "\ton lemma.id = idx_t.lemma_id WHERE idx_t.id is null)\n" +
            "AS null_lemma) LIMIT 1000000", nativeQuery = true)
    void deleteLemma();

    @Query(value = "SELECT if(lem.id is null, count(*), lem.id) FROM search_engine.lemma AS lem " +
            "WHERE lem.lemma = :lemma and lem.site_id = :siteId", nativeQuery = true)
    int getId(String lemma, int siteId);

    @Query(value = "SELECT se.lemma\n" +
            "FROM search_engine.lemma AS se\n" +
            "JOIN (SELECT site_id, sum(frequency) AS freq FROM search_engine.lemma GROUP BY site_id) AS total\n" +
            "ON se.site_id = total.site_id\n" +
            "WHERE se.lemma IN (:lemmas) AND total.freq <> 0 and round(frequency * 100 / total.freq) = 0\n" +
            "ORDER BY se.frequency;", nativeQuery = true)
    String[] getUniqueLemma(String[] lemmas);

    @Query(value = "SELECT idx.page_id FROM search_engine.index_table AS idx\n" +
            "JOIN lemma AS lm ON idx.lemma_id = lm.id\n" +
            "WHERE lm.lemma = :lemma", nativeQuery = true)
    Integer[] getIdPages(String lemma);
}
