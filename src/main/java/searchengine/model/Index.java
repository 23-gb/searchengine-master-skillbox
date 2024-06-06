package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "index_words")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="page_id", nullable=false, foreignKey = @ForeignKey(name = "FK_index_page"))
    private Page page;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lemma_id", nullable=false, foreignKey = @ForeignKey(name = "FK_index_lemma"))
    private Lemma lemma;

    @Column(name="rank_index", nullable = false)
    private float rank;
}
