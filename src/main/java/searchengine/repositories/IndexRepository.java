package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<Index, Integer> {
    @Query("SELECT i FROM Index AS i WHERE i.page =:page")
    List<Index> findByPage(Page page);
    @Query("SELECT i.page FROM Index AS i WHERE i.lemma =:lemma")
    List<Page> findPagesByLemma(Lemma lemma);
    List<Index> findByLemma(Lemma lemma);
    @Query("SELECT i FROM Index AS i WHERE i.lemma IN (:lemmas) AND i.page IN (:pages) ")
    List<Index> findByLemmasAndPages(List<Lemma> lemmas, List<Page> pages);
}
