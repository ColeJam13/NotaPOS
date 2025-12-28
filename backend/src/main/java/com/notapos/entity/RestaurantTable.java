package com.notapos.entity;

import java.time.LocalDateTime;                   // JPA annotations

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entity representing a physical table in the restaurant.
 * 
 * Tracks table metadata, current status, and assignment.
 * Status changes are logged in TableStatusLog for audit trail.
 * 
 * @author CJ
 */

@Entity                                     // Tells JPA "this is a database table"
@Table(name = "tables")                     // table name in database is "tables"

public class RestaurantTable {

    @Id                                                         // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)         // auto-increment ID value
    @Column(name = "table_id")                                  // column name in database
    private Long tableId;                                       // field name in java

    @Column(name = "table_number", nullable = false, unique = true, length = 10)        // nullable = false (means required field) - unique = true (means no duplicate table numbers)
    private String tableNumber;

    @Column(name = "section", length = 20)
    private String section;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "available";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RestaurantTable() {}

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}