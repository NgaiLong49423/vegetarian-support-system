package tech.mamxanh.recipe.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Read-only recipe-module view of the authenticated author's existing USER row. */
@Entity
@Table(name = "\"USER\"")
@Getter
@NoArgsConstructor
public class RecipeAuthorReferenceEntity {
    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;
}
