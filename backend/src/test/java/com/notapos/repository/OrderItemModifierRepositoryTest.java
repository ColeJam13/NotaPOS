package com.notapos.repository;

import com.notapos.entity.OrderItemModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for OrderItemModifierRepository.
 * 
 * Tests database queries for order item to modifier relationships.
 * 
 * @author CJ
 */

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class OrderItemModifierRepositoryTest {

    @Autowired
    private OrderItemModifierRepository orderItemModifierRepository;

    private OrderItemModifier modifier1;
    private OrderItemModifier modifier2;
    private OrderItemModifier modifier3;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        orderItemModifierRepository.deleteAll();

        // Create modifier: Order item 1 has Fries (no upcharge)
        modifier1 = new OrderItemModifier();
        modifier1.setOrderItemId(1L);
        modifier1.setModifierId(1L);
        modifier1.setPriceAdjustment(BigDecimal.ZERO);
        modifier1 = orderItemModifierRepository.save(modifier1);

        // Create modifier: Order item 1 has Add Bacon (+$2.00)
        modifier2 = new OrderItemModifier();
        modifier2.setOrderItemId(1L);
        modifier2.setModifierId(2L);
        modifier2.setPriceAdjustment(new BigDecimal("2.00"));
        modifier2 = orderItemModifierRepository.save(modifier2);

        // Create modifier: Order item 2 has Salad (no upcharge)
        modifier3 = new OrderItemModifier();
        modifier3.setOrderItemId(2L);
        modifier3.setModifierId(3L);
        modifier3.setPriceAdjustment(BigDecimal.ZERO);
        modifier3 = orderItemModifierRepository.save(modifier3);
    }

    @Test
    void testSave_ShouldPersistModifier() {
        // WHAT: Test saving a new order item modifier
        // WHY: Ensure basic create operation works
        
        // Given - New modifier selection
        OrderItemModifier newModifier = new OrderItemModifier();
        newModifier.setOrderItemId(3L);
        newModifier.setModifierId(4L);
        newModifier.setPriceAdjustment(new BigDecimal("1.50"));

        // When - Save to database
        OrderItemModifier saved = orderItemModifierRepository.save(newModifier);

        // Then - Should persist with generated ID
        assertNotNull(saved.getOrderItemModifierId());
        assertEquals(3L, saved.getOrderItemId());
        assertEquals(4L, saved.getModifierId());
    }

    @Test
    void testFindById_WhenExists_ShouldReturnModifier() {
        // WHAT: Test finding modifier by ID
        // WHY: Need to load specific modifier selections
        
        // Given - Modifier exists in database (from setUp)
        
        // When - Find by ID
        Optional<OrderItemModifier> result = orderItemModifierRepository.findById(modifier1.getOrderItemModifierId());

        // Then - Should find the modifier
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getOrderItemId());
        assertEquals(1L, result.get().getModifierId());
    }

    @Test
    void testFindById_WhenNotExists_ShouldReturnEmpty() {
        // WHAT: Test finding non-existent modifier
        // WHY: Handle missing modifiers gracefully
        
        // Given - Non-existent ID
        
        // When - Try to find
        Optional<OrderItemModifier> result = orderItemModifierRepository.findById(999L);

        // Then - Should return empty
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll_ShouldReturnAllModifiers() {
        // WHAT: Test retrieving all order item modifiers
        // WHY: Get complete history of all modifier selections
        
        // Given - 3 modifiers in database (from setUp)
        
        // When - Find all
        List<OrderItemModifier> modifiers = orderItemModifierRepository.findAll();

        // Then - Should get all 3 modifiers
        assertEquals(3, modifiers.size());
    }

    @Test
    void testFindByOrderItemId_ShouldReturnModifiersForItem() {
        // WHAT: Test finding all modifiers for an order item
        // WHY: Show what customizations customer selected for their Chicken Cutty
        
        // Given - Order item 1 has 2 modifiers (Fries + Add Bacon from setUp)
        
        // When - Find modifiers for order item 1
        List<OrderItemModifier> itemModifiers = orderItemModifierRepository.findByOrderItemId(1L);

        // Then - Should get 2 modifiers
        assertEquals(2, itemModifiers.size());
        assertTrue(itemModifiers.stream().allMatch(m -> m.getOrderItemId().equals(1L)));
    }

    @Test
    void testFindByModifierId_ShouldReturnOrderItemsWithModifier() {
        // WHAT: Test finding all order items that have a specific modifier
        // WHY: Track how many orders included "Add Bacon" (analytics)
        
        // Given - Modifier 2 (Add Bacon) is on 1 order item (from setUp)
        
        // When - Find order items with modifier 2
        List<OrderItemModifier> itemsWithModifier = orderItemModifierRepository.findByModifierId(2L);

        // Then - Should get 1 order item
        assertEquals(1, itemsWithModifier.size());
        assertEquals(2L, itemsWithModifier.get(0).getModifierId());
    }

    @Test
    void testDeleteById_ShouldRemoveModifier() {
        // WHAT: Test deleting an order item modifier
        // WHY: Remove modifier selections (not typical in production)
        
        // Given - Modifier exists
        Long modifierId = modifier3.getOrderItemModifierId();
        
        // When - Delete the modifier
        orderItemModifierRepository.deleteById(modifierId);

        // Then - Modifier should no longer exist
        Optional<OrderItemModifier> deleted = orderItemModifierRepository.findById(modifierId);
        assertFalse(deleted.isPresent());
    }

    @Test
    void testModifierWithPriceAdjustment_ShouldStoreUpcharge() {
        // WHAT: Test that price adjustments are stored correctly
        // WHY: Track upcharge for Add Bacon at time of order (+$2.00)
        
        // Given - Add Bacon modifier has price adjustment (from setUp)
        
        // When - Load Add Bacon modifier
        OrderItemModifier loaded = orderItemModifierRepository.findById(modifier2.getOrderItemModifierId()).orElseThrow();

        // Then - Price adjustment should be preserved
        assertEquals(new BigDecimal("2.00"), loaded.getPriceAdjustment());
        assertEquals(1L, loaded.getOrderItemId());
    }

    @Test
    void testModifierWithNoPriceAdjustment_ShouldStoreZero() {
        // WHAT: Test that free modifiers store zero price adjustment
        // WHY: Track Fries selection with no upcharge ($0.00)
        
        // Given - Fries modifier has no upcharge (from setUp)
        
        // When - Load Fries modifier
        OrderItemModifier loaded = orderItemModifierRepository.findById(modifier1.getOrderItemModifierId()).orElseThrow();

        // Then - Price adjustment should be zero
        assertEquals(BigDecimal.ZERO, loaded.getPriceAdjustment());
    }

    @Test
    void testMultipleModifiersForOneItem_ShouldAllowMultipleSelections() {
        // WHAT: Test that one order item can have multiple modifiers
        // WHY: Chicken Cutty can have both "Fries" and "Add Bacon"
        
        // Given - Order item 1 has 2 modifiers (from setUp)
        
        // When - Find all modifiers for order item 1
        List<OrderItemModifier> modifiers = orderItemModifierRepository.findByOrderItemId(1L);

        // Then - Should have 2 different modifiers
        assertEquals(2, modifiers.size());
        assertEquals(1L, modifiers.get(0).getModifierId());
        assertEquals(2L, modifiers.get(1).getModifierId());
    }

    @Test
    void testHistoricalPricing_ShouldPreservePriceAtTimeOfOrder() {
        // WHAT: Test that price adjustments are captured at order time
        // WHY: Even if "Add Bacon" price changes later, order shows $2.00
        
        // Given - Modifier with price adjustment saved (from setUp)
        Long modifierId = modifier2.getOrderItemModifierId();
        
        // When - Load modifier later
        OrderItemModifier historical = orderItemModifierRepository.findById(modifierId).orElseThrow();

        // Then - Price adjustment is preserved from time of order
        assertEquals(new BigDecimal("2.00"), historical.getPriceAdjustment());
        // Even if current modifier price changes, this order's price stays $2.00
    }
}