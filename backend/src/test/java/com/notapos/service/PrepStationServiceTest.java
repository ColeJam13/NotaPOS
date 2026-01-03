package com.notapos.service;

import com.notapos.entity.PrepStation;
import com.notapos.repository.PrepStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrepStationService.
 * 
 * Tests prep station management (Kitchen, Bar, etc.).
 * 
 * @author CJ
 */

@ExtendWith(MockitoExtension.class)
class PrepStationServiceTest {

    @Mock
    private PrepStationRepository prepStationRepository;

    @InjectMocks
    private PrepStationService prepStationService;

    private PrepStation testStation;

    @BeforeEach
    void setUp() {
        // Create a test prep station (Kitchen)
        testStation = new PrepStation();
        testStation.setPrepStationId(1L);
        testStation.setName("Kitchen");
        testStation.setDescription("Main kitchen prep station");
        testStation.setIsActive(true);
    }

    @Test
    void testCreatePrepStation_ShouldSaveStation() {
        // WHAT: Test creating a new prep station
        // WHY: Need to set up stations where food/drinks are made
        
        // Given - Mock returns saved station
        when(prepStationRepository.save(any(PrepStation.class))).thenReturn(testStation);

        // When - Create prep station
        PrepStation created = prepStationService.createPrepStation(testStation);

        // Then - Should save and return station
        assertNotNull(created);
        assertEquals("Kitchen", created.getName());
        assertTrue(created.getIsActive());
        verify(prepStationRepository, times(1)).save(testStation);
    }

    @Test
    void testGetAllPrepStations_ShouldReturnAllStations() {
        // WHAT: Test retrieving all prep stations (active and inactive)
        // WHY: Admin needs to see all configured stations
        
        // Given - Mock returns 2 stations
        PrepStation barStation = new PrepStation();
        barStation.setName("Bar");
        List<PrepStation> stations = Arrays.asList(testStation, barStation);
        when(prepStationRepository.findAll()).thenReturn(stations);

        // When - Get all stations
        List<PrepStation> result = prepStationService.getAllPrepStations();

        // Then - Should get both stations
        assertEquals(2, result.size());
        verify(prepStationRepository, times(1)).findAll();
    }

    @Test
    void testGetActivePrepStations_ShouldReturnOnlyActive() {
        // WHAT: Test getting only active prep stations
        // WHY: Only route orders to active stations
        
        // Given - Mock returns only active stations
        List<PrepStation> activeStations = Arrays.asList(testStation);
        when(prepStationRepository.findByIsActive(true)).thenReturn(activeStations);

        // When - Get active stations
        List<PrepStation> result = prepStationService.getActivePrepStations();

        // Then - Should get only active stations
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(prepStationRepository, times(1)).findByIsActive(true);
    }

    @Test
    void testGetPrepStationById_WhenExists_ShouldReturnStation() {
        // WHAT: Test finding a specific prep station by ID
        // WHY: Need to load station details
        
        // Given - Mock returns the station
        when(prepStationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        // When - Get station by ID
        Optional<PrepStation> result = prepStationService.getPrepStationById(1L);

        // Then - Should find the station
        assertTrue(result.isPresent());
        assertEquals("Kitchen", result.get().getName());
        verify(prepStationRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPrepStationByName_ShouldReturnStation() {
        // WHAT: Test finding prep station by name
        // WHY: Look up station by name (Kitchen, Bar, etc.)
        
        // Given - Mock returns station by name
        when(prepStationRepository.findByName("Kitchen")).thenReturn(Optional.of(testStation));

        // When - Find station by name
        Optional<PrepStation> result = prepStationService.getPrepStationByName("Kitchen");

        // Then - Should find the station
        assertTrue(result.isPresent());
        assertEquals("Kitchen", result.get().getName());
        verify(prepStationRepository, times(1)).findByName("Kitchen");
    }

    @Test
    void testUpdatePrepStation_ShouldUpdateFields() {
        // WHAT: Test updating prep station details
        // WHY: Change station name, description, or active status
        
        // Given - Station exists
        PrepStation updatedData = new PrepStation();
        updatedData.setName("Updated Kitchen");
        updatedData.setDescription("New description");
        updatedData.setIsActive(true);

        when(prepStationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(prepStationRepository.save(any(PrepStation.class))).thenReturn(testStation);

        // When - Update station
        PrepStation result = prepStationService.updatePrepStation(1L, updatedData);

        // Then - Should update all fields
        assertEquals("Updated Kitchen", result.getName());
        assertEquals("New description", result.getDescription());
        verify(prepStationRepository, times(1)).save(testStation);
    }

    @Test
    void testUpdatePrepStation_WhenNotFound_ShouldThrowException() {
        // WHAT: Test error handling when station doesn't exist
        // WHY: Can't update non-existent station
        
        // Given - Station doesn't exist
        when(prepStationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then - Should throw exception
        assertThrows(RuntimeException.class, () -> {
            prepStationService.updatePrepStation(999L, new PrepStation());
        });
    }

    @Test
    void testDeletePrepStation_ShouldCallRepository() {
        // WHAT: Test deleting a prep station
        // WHY: Remove unused stations (rare)
        
        // Given - Mock repository
        doNothing().when(prepStationRepository).deleteById(1L);

        // When - Delete station
        prepStationService.deletePrepStation(1L);

        // Then - Repository delete should be called
        verify(prepStationRepository, times(1)).deleteById(1L);
    }
}