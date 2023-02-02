package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static javax.persistence.GenerationType.IDENTITY;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "explore_categories")
public class ExploreCategories {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    @Column(name = "category")
    private String category;

    @Column(name = "included")
    private Boolean included = true;

    @Column(name = "category_order")
    private Integer order = 0;

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;

        if (!(other instanceof ExploreCategories)) return false;

        final ExploreCategories that = (ExploreCategories) other;

        return new EqualsBuilder()
                .append(category, that.category)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(category)
                .toHashCode();
    }

}
