package com.aws.carddemo.billing;

import java.io.Serializable;
import java.util.Objects;

public class CategoryBalanceId implements Serializable {

    private Long account;
    private String category;

    public CategoryBalanceId() {
    }

    public CategoryBalanceId(Long account, String category) {
        this.account = account;
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryBalanceId that = (CategoryBalanceId) o;
        return Objects.equals(account, that.account) && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, category);
    }
}
