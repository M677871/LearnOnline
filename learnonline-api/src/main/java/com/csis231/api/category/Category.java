package com.csis231.api.category;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * JPA entity representing a course category in the online learning platform
 * (for example "Programming", "Mathematics", "Design").
 *
 * <p>The {@code name} field is unique and used as a human-readable label
 * when grouping courses.</p>
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
        )
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@ToString
@Getter
public class Category {

    /** Primary key of the category. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique name of the category shown to users. */
    @NotBlank
    private String name;
}
