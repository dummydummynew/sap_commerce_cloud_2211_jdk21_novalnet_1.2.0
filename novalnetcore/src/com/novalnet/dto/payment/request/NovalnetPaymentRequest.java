package com.novalnet.dto.payment.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NovalnetPaymentRequest implements Serializable {
    private Merchant merchant;
    private Customer customer;
    private Transaction transaction;
    private Custom custom;

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(final Merchant merchant) {
        this.merchant = merchant;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(final Customer customer) {
        this.customer = customer;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(final Transaction transaction) {
        this.transaction = transaction;
    }

    public Custom getCustom() {
        return custom;
    }

    public void setCustom(final Custom custom) {
        this.custom = custom;
    }
}
