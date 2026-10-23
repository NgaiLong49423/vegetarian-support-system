package tech.mamxanh.shopping.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"SHOPPING_LIST\"")
@Getter @Setter @NoArgsConstructor
public class ShoppingListEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_list_id") private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "list_kind", nullable = false, length = 20) private String kind = "MEAL_PLAN";
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false) private LocalDateTime updatedAt;
}
