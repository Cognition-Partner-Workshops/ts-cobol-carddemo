package com.carddemo.transactiontype.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_type")
public class TransactionType {

    @Id
    @Column(name = "tr_type", length = 2, nullable = false)
    @NotBlank(message = "Transaction type code is required")
    @Size(min = 1, max = 2, message = "Transaction type code must be 1-2 characters")
    private String trType;

    @Column(name = "tr_description", length = 50)
    @Size(max = 50, message = "Description must not exceed 50 characters")
    private String trDescription;

    @JsonIgnore
    @OneToMany(mappedBy = "transactionType")
    private List<TransactionTypeCategory> categories = new ArrayList<>();

    public TransactionType() {
    }

    public TransactionType(String trType, String trDescription) {
        this.trType = trType;
        this.trDescription = trDescription;
    }

    public String getTrType() {
        return trType;
    }

    public void setTrType(String trType) {
        this.trType = trType;
    }

    public String getTrDescription() {
        return trDescription;
    }

    public void setTrDescription(String trDescription) {
        this.trDescription = trDescription;
    }

    public List<TransactionTypeCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<TransactionTypeCategory> categories) {
        this.categories = categories;
    }
}
