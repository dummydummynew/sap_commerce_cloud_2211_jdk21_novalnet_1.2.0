package com.novalnet.dto.payment.request;

public class Merchant {
    private String signature;
    private String tariff;

    public String getSignature() {
        return signature;
    }

    public void setSignature(final String signature) {
        this.signature = signature;
    }

    public String getTariff() {
        return tariff;
    }

    public void setTariff(final String tariff) {
        this.tariff = tariff;
    }
}
