package com.poultry.pms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A farm (or an out-grower site) that owns flocks. */
@Entity
@Table(name = "farm")
public class Farm extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "location")
    private String location;

    @Column(name = "contact_notes")
    private String contactNotes;

    protected Farm() { }   // required by JPA

    public Farm(String name, String location, String contactNotes) {
        this.name = name;
        this.location = location;
        this.contactNotes = contactNotes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getContactNotes() { return contactNotes; }
    public void setContactNotes(String contactNotes) { this.contactNotes = contactNotes; }
}
