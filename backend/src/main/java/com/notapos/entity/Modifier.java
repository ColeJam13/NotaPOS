package com.notapos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a modifier option.
 * 
 * Modifiers are the actual choices within a modifier group.
 * Example: "Fries", "Salad", "Add Bacon +$2"
 * 
 * @author CJ
 */

@Entity
@Table(name = "modifiers")
public class Modifier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "modifier_id")
    private Long modifierId;

    @Column(name = "modifier_group_id", nullable = false)
    private Long modifierGroupId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price_adjustment", precision = 10, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Modifier() {}

    public Long getModifierId() {
        return modifierId;
    }

    public void setModifierId(Long modifierId) {
        this.modifierId = modifierId;
    }

    public Long getModifierGroupId() {
        return modifierGroupId;
    }

    public void setModifierGroupId(Long modifierGroupId) {
        this.modifierGroupId = modifierGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
