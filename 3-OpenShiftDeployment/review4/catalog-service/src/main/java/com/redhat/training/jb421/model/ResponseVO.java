package com.redhat.training.jb421.model;

import java.io.Serializable;

public class ResponseVO implements Serializable {

    private Integer id;
    private String description;
    private String author;
    private String vendorName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }


    @Override
    public String toString() {
        return "ResponseVO{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", author='" + author + '\'' +
                ", vendorName='" + vendorName + '\'' +
                '}';
    }
}
