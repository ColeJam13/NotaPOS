package com.notapos.service;

import com.notapos.entity.Modifier;
import com.notapos.repository.ModifierRepository;
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
 * Unit tests for ModifierService.
 * 
 * Tests modifier management (Fries, Add Bacon +$2, etc.).
 * 
 * @author CJ
 */

@ExtendWith(MockitoExtension.class)
class ModifierServiceTest {

    @Mock
    private ModifierRepository modifierRepository;

    @InjectMocks
    private ModifierService modifierService;

    private Modifier testModifier;

    @BeforeEach
    void setUp() {
        // Create a test modifier (Fries - no extra charge)
        testModifier = new Modifier();
        testModifier.setModifierId(1L);
        testModifier.setModifierGroupId(1L);
        testModifier.setName("Fries");
        testModifier.setPriceAdjustment(BigDecimal.ZERO);
        testModifier.setIsActive(true);
    }

    @Test
    void testCreateModifier_ShouldSaveModifier() {
        // WHAT: Test creating a new modifier
        // WHY: Need to add customization options (Fries, Salad, Add Bacon)
        
        // Given - Mock returns saved modifier
        when(modifierRepository.save(any(Modifier.class))).thenReturn(testModifier);

        // When - Create modifier
        Modifier created = modifierService.createModifier(testModifier);

        // Then - Should save and return modifier
        assertNotNull(created);
        assertEquals("Fries", created.getName());
        assertEquals(BigDecimal.ZERO, created.getPriceAdjustment());
        assertTrue(created.getIsActive());
        verify(modifierRepository, times(1)).save(testModifier);
    }

    @Test
    void testGetAllModifiers_ShouldReturnAllModifiers() {
        // WHAT: Test retrieving all modifiers (active and inactive)
        // WHY: Admin needs to see all configured modifiers
        
        // Given - Mock returns 2 modifiers
        Modifier modifier2 = new Modifier();
        modifier2.setName("Salad");
        List<Modifier> modifiers = Arrays.asList(testModifier, modifier2);
        when(modifierRepository.findAll()).thenReturn(modifiers);

        // When - Get all modifiers
        List<Modifier> result = modifierService.getAllModifiers();

        // Then - Should get both modifiers
        assertEquals(2, result.size());
        verify(modifierRepository, times(1)).findAll();
    }

    @Test
    void testGetActiveModifiers_ShouldReturnOnlyActive() {
        // WHAT: Test getting only active modifiers
        // WHY: Only show available options to servers
        
        // Given - Mock returns only active modifiers
        List<Modifier> activeModifiers = Arrays.asList(testModifier);
        when(modifierRepository.findByIsActive(true)).thenReturn(activeModifiers);

        // When - Get active modifiers
        List<Modifier> result = modifierService.getActiveModifiers();

        // Then - Should get only active modifiers
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(modifierRepository, times(1)).findByIsActive(true);
    }

    @Test
    void testGetModifierById_WhenExists_ShouldReturnModifier() {
        // WHAT: Test finding a specific modifier by ID
        // WHY: Need to load modifier details
        
        // Given - Mock returns the modifier
        when(modifierRepository.findById(1L)).thenReturn(Optional.of(testModifier));

        // When - Get modifier by ID
        Optional<Modifier> result = modifierService.getModifierById(1L);

        // Then - Should find the modifier
        assertTrue(result.isPresent());
        assertEquals("Fries", result.get().getName());
        verify(modifierRepository, times(1)).findById(1L);
    }

    @Test
    void testGetModifiersByGroup_ShouldReturnGroupModifiers() {
        // WHAT: Test getting all modifiers for a specific group
        // WHY: Show all options in "Choose a Side" group
        
        // Given - Mock returns modifiers for group 1
        List<Modifier> groupModifiers = Arrays.asList(testModifier);
        when(modifierRepository.findByModifierGroupId(1L)).thenReturn(groupModifiers);

        // When - Get modifiers for group 1
        List<Modifier> result = modifierService.getModifiersByGroup(1L);

        // Then - Should get that group's modifiers
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getModifierGroupId());
        verify(modifierRepository, times(1)).findByModifierGroupId(1L);
    }

    @Test
    void testGetActiveModifiersByGroup_ShouldFilterByBoth() {
        // WHAT: Test getting active modifiers for a specific group
        // WHY: Show only available options in a group
        
        // Given - Mock returns active modifiers for group 1
        List<Modifier> activeModifiers = Arrays.asList(testModifier);
        when(modifierRepository.findByModifierGroupIdAndIsActive(1L, true)).thenReturn(activeModifiers);

        // When - Get active modifiers for group 1
        List<Modifier> result = modifierService.getActiveModifiersByGroup(1L);

        // Then - Should get active modifiers in that group
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getModifierGroupId());
        assertTrue(result.get(0).getIsActive());
        verify(modifierRepository, times(1)).findByModifierGroupIdAndIsActive(1L, true);
    }

    @Test
    void testCreateModifier_WithPriceAdjustment_ShouldSaveWithPrice() {
        // WHAT: Test creating modifier with price adjustment
        // WHY: Some modifiers cost extra (Add Bacon +$2, Extra Sauce +$1)
        
        // Given - Modifier with price adjustment
        Modifier baconModifier = new Modifier();
        baconModifier.setName("Add Bacon");
        baconModifier.setPriceAdjustment(new BigDecimal("2.00"));
        baconModifier.setIsActive(true);
        
        when(modifierRepository.save(any(Modifier.class))).thenReturn(baconModifier);

        // When - Create modifier
        Modifier created = modifierService.createModifier(baconModifier);

        // Then - Should save with price adjustment
        assertEquals("Add Bacon", created.getName());
        assertEquals(new BigDecimal("2.00"), created.getPriceAdjustment());
    }

    @Test
    void testUpdateModifier_ShouldUpdateFields() {
        // WHAT: Test updating modifier details
        // WHY: Change modifier name or price adjustment
        
        // Given - Modifier exists
        Modifier updatedData = new Modifier();
        updatedData.setName("Updated Fries");
        updatedData.setPriceAdjustment(new BigDecimal("1.00"));
        updatedData.setIsActive(true);

        when(modifierRepository.findById(1L)).thenReturn(Optional.of(testModifier));
        when(modifierRepository.save(any(Modifier.class))).thenReturn(testModifier);

        // When - Update modifier
        Modifier result = modifierService.updateModifier(1L, updatedData);

        // Then - Should update all fields
        assertEquals("Updated Fries", result.getName());
        assertEquals(new BigDecimal("1.00"), result.getPriceAdjustment());
        verify(modifierRepository, times(1)).save(testModifier);
    }

    @Test
    void testUpdateModifier_WhenNotFound_ShouldThrowException() {
        // WHAT: Test error handling when modifier doesn't exist
        // WHY: Can't update non-existent modifier
        
        // Given - Modifier doesn't exist
        when(modifierRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then - Should throw exception
        assertThrows(RuntimeException.class, () -> {
            modifierService.updateModifier(999L, new Modifier());
        });
    }

    @Test
    void testDeleteModifier_ShouldCallRepository() {
        // WHAT: Test deleting a modifier
        // WHY: Remove unused customization options
        
        // Given - Mock repository
        doNothing().when(modifierRepository).deleteById(1L);

        // When - Delete modifier
        modifierService.deleteModifier(1L);

        // Then - Repository delete should be called
        verify(modifierRepository, times(1)).deleteById(1L);
    }
}
