package br.com.eric.usermanagement.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "addresses")
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE addresses SET deleted = true WHERE id = ?")
@Getter
@Setter
@NoArgsConstructor
public class Address extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String cep;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String number;

    private String complement;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String city;

    private String district;

    @Column(name = "main_address", nullable = false)
    private boolean mainAddress;
}
