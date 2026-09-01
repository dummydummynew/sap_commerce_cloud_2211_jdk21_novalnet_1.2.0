package com.novalnet.dto.payment.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Billing {
    private String first_name;
    private String last_name;
    private String street;
    private String house_no;
	 private String tel;
    private String city;
    private String zip;
    private String country_code;

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(final String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(final String last_name) {
        this.last_name = last_name;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public String getHouse_no() {
        return house_no;
    }

    public void setHouse_no(final String house_no) {
        this.house_no = house_no;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(final String zip) {
        this.zip = zip;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(final String country_code) {
        this.country_code = country_code;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(final String tel) {
        this.tel = tel;
    }
}
