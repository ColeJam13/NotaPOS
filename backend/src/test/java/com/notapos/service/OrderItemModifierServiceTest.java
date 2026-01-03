package com.notapos.service;

import com.notapos.entity.OrderItemModifier;
import com.notapos.repository.OrderItemModifierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderItemModifierService.
 * 
 * Tests tracking which modifiers were selected for order items.
 * 
 * @author CJ
 */

@ExtendWith(MockitoExtension.class)
class OrderItemModifierServiceTest {

    @Mock
    private OrderItemModifierRepository orderItemModifierRepository;

    @InjectMocks
    private OrderItemModifierService orderItemModifierService;

    private OrderItemModifier testOrderItemModifier;

    @BeforeEach
    void setUp() {
        // Create a test order item modifier (Chicken Cutty with Fries)
        testOrderItemModifier = new OrderItemModifier();
        testOrderItemModifier.setOrderItemModifierId(1L);
        testOrderItemModifier.setOrderItemId(1L);
        testOrderItemModifier.setModifierId(1L);
        testOrderItemModifier.setPriceAdjustment(BigDecimal.ZERO);
    }

    @Test
    void testCreateOrderItemModifier_ShouldSaveModifier() {
        // WHAT: Test creating a new order item modifier
        // WHY: Track which modifiers customer selected (Fries, Add Bacon, etc.)
        
        // Given - Mock returns saved modifier
        when(orderItemModifierRepository.save(any(OrderItemModifier.class))).thenReturn(testOrderItemModifier);

        // When - Create order item modifier
        OrderItemModifier created = orderItemModifierService.createOrderItemModifier(testOrderItemModifier);

        // Then - Should save and return modifier
        assertNotNull(created);
        assertEquals(1L, created.getOrderItemId());
        assertEquals(1L, created.getModifierId());
        assertEquals(BigDecimal.ZERO, created.getPriceAdjustment());
        verify(orderItemModifierRepository, times(1)).save(testOrderItemModifier);
    }

    @Test
    void testCreateOrderItemModifier_WithPriceAdjustment_ShouldCapturePricing() {
        // WHAT: Test creating modifier with price adjustment
        // WHY: Track historical pricing (Add Bacon was +$2 at time of order)
        
        // Given - Modifier with price adjustment
        OrderItemModifier baconModifier = new OrderItemModifier();
        baconModifier.setOrderItemId(1L);
        baconModifier.setModifierId(2L);
        baconModifier.setPriceAdjustment(new BigDecimal("2.00"));
        
        when(orderItemModifierRepository.save(any(OrderItemModifier.class))).thenReturn(baconModifier);

        // When - Create modifier
        OrderItemModifier created = orderItemModifierService.createOrderItemModifier(baconModifier);

        // Then - Should save with price adjustment
        assertEquals(new BigDecimal("2.00"), created.getPriceAdjustment());
    }

    @Test
    void testGetAllOrderItemModifiers_ShouldReturnAllModifiers() {
        // WHAT: Test retrieving all order item modifiers
        // WHY: View all modifier selections across all orders
        
        // Given - Mock returns 2 modifiers
        OrderItemModifier modifier2 = new OrderItemModifier();
        List<OrderItemModifier> modifiers = Arrays.asList(testOrderItemModifier, modifier2);
        when(orderItemModifierRepository.findAll()).thenReturn(modifiers);

        // When - Get all modifiers
        List<OrderItemModifier> result = orderItemModifierService.getAllOrderItemModifiers();

        // Then - Should get both modifiers
        assertEquals(2, result.size());
        verify(orderItemModifierRepository, times(1)).findAll();
    }

    @Test
    void testGetOrderItemModifierById_WhenExists_ShouldReturnModifier() {
        // WHAT: Test finding a specific modifier by ID
        // WHY: Need to load modifier details
        
        // Given - Mock returns the modifier
        when(orderItemModifierRepository.findById(1L)).thenReturn(Optional.of(testOrderItemModifier));

        // When - Get modifier by ID
        Optional<OrderItemModifier> result = orderItemModifierService.getOrderItemModifierById(1L);

        // Then - Should find the modifier
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getOrderItemId());
        verify(orderItemModifierRepository, times(1)).findById(1L);
    }

    @Test
    void testGetModifiersForOrderItem_ShouldReturnItemModifiers() {
        // WHAT: Test getting all modifiers for a specific order item
        // WHY: See what customer selected (Fries + Add Bacon + Extra Sauce)
        
        // Given - Mock returns modifiers for order item
        OrderItemModifier modifier2 = new OrderItemModifier();
        modifier2.setOrderItemId(1L);
        modifier2.setModifierId(2L);
        modifier2.setPriceAdjustment(new BigDecimal("2.00"));
        
        List<OrderItemModifier> itemModifiers = Arrays.asList(testOrderItemModifier, modifier2);
        when(orderItemModifierRepository.findByOrderItemId(1L)).thenReturn(itemModifiers);

        // When - Get modifiers for order item 1
        List<OrderItemModifier> result = orderItemModifierService.getModifiersForOrderItem(1L);

        // Then - Should get all modifiers for that item
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getOrderItemId());
        assertEquals(1L, result.get(1).getOrderItemId());
        verify(orderItemModifierRepository, times(1)).findByOrderItemId(1L);
    }

    @Test
    void testGetOrderItemsWithModifier_ShouldReturnMatchingItems() {
        // WHAT: Test getting all order items that used a specific modifier
        // WHY: Analytics (how many people ordered "Add Bacon"?)
        
        // Given - Mock returns order items with bacon modifier
        List<OrderItemModifier> baconOrders = Arrays.asList(testOrderItemModifier);
        when(orderItemModifierRepository.findByModifierId(2L)).thenReturn(baconOrders);

        // When - Get all order items with bacon
        List<OrderItemModifier> result = orderItemModifierService.getOrderItemsWithModifier(2L);

        // Then - Should get all items with that modifier
        assertEquals(1, result.size());
        verify(orderItemModifierRepository, times(1)).findByModifierId(2L);
    }

    @Test
    void testDeleteOrderItemModifier_ShouldCallRepository() {
        // WHAT: Test deleting an order item modifier
        // WHY: Remove modifier selections (if order item is edited)
        
        // Given - Mock repository
        doNothing().when(orderItemModifierRepository).deleteById(1L);

        // When - Delete modifier
        orderItemModifierService.deleteOrderItemModifier(1L);

        // Then - Repository delete should be called
        verify(orderItemModifierRepository, times(1)).deleteById(1L);
    }
}