package com.cartlc.tracker.data;

import android.text.TextUtils;

/**
 * Created by dug on 5/10/17.
 */

public class DataAddress {
    public long    id;
    public int     server_id;
    public String  company;
    public String  street;
    public String  city;
    public String  state;
    public String  zipcode;
    public boolean disabled;
    public boolean isLocal;
    public boolean isTest;

    public DataAddress(String company, String street, String city, String state) {
        this.company = company;
        this.street = street;
        this.city = city;
        this.state = state;
        this.isTest = true;
    }

    public DataAddress(String company, String street, String city, String state, String zipcode) {
        this.company = company;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    public DataAddress(String company, String zipcode) {
        this.company = company;
        this.zipcode = zipcode;
    }

    public DataAddress(int server_id, String company, String street, String city, String state, String zipcode) {
        this.server_id = server_id;
        this.company = company;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    public DataAddress(long id, int server_id, String company, String street, String city, String state, String zipcode) {
        this.id = id;
        this.server_id = server_id;
        this.company = company;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    public String getBlock() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(company);

        if (hasAddress()) {
            sbuf.append("\n");
            sbuf.append(street);
            sbuf.append(",\n");
            sbuf.append(city);
            sbuf.append(", ");
            sbuf.append(state);
        }
        if (!TextUtils.isEmpty(zipcode)) {
            sbuf.append(" ");
            sbuf.append(zipcode);
        }
        return sbuf.toString();
    }

    // Used to send address to the server.
    public String getLine() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(company);

        if (hasAddress()) {
            sbuf.append(", ");
            sbuf.append(street);
            sbuf.append(", ");
            sbuf.append(city);
            sbuf.append(", ");
            sbuf.append(state);
        }
        if (!TextUtils.isEmpty(zipcode)) {
            sbuf.append(", ");
            sbuf.append(zipcode);
        }
        return sbuf.toString();
    }

    boolean hasAddress() {
        return (street != null && street.length() > 0) ||
                (city != null && city.length() > 0) ||
                (state != null && state.length() > 0);
    }

    boolean hasZipCode() {
        return !TextUtils.isEmpty(zipcode);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataAddress) {
            return equals((DataAddress) obj);
        }
        if (obj instanceof Integer) {
            return id == (Integer) obj;
        }
        return false;
    }

    static boolean equals(String field1, String field2) {
        if (field1 == null && field2 == null) {
            return true;
        }
        if (field1 != null && field2 == null) {
            return false;
        }
        if (field1 == null && field2 != null) {
            return false;
        }
        return field1.equals(field2);
    }

    public boolean equals(DataAddress item) {
        return equals(company, item.company) &&
                equals(street, item.street) &&
                equals(city, item.city) &&
                equals(state, item.state) &&
                equals(zipcode, item.zipcode);
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(company);
        sbuf.append(", ");
        sbuf.append(street);
        sbuf.append(", ");
        sbuf.append(city);
        sbuf.append(", ");
        sbuf.append(state);
        sbuf.append(", ");
        sbuf.append(zipcode);
        sbuf.append(", ");
        sbuf.append(", L(");
        sbuf.append(isLocal);
        sbuf.append("), D(");
        sbuf.append(disabled);
        sbuf.append(")");
        sbuf.append(", S[");
        sbuf.append(server_id);
        sbuf.append("]");
        return sbuf.toString();
    }
}
