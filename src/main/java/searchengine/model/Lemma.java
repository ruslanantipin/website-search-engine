package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Setter
@Getter
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "site_id", "lemma" }) })
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    //@Column(name = "site_id", nullable = false)
    //private int siteId;

    //@Column(unique = true)
    @ManyToOne(cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteTable site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String lemma;

    @Column(nullable = false)
    private int frequency;
}
