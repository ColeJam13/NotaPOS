package com.notapos.repository;

import com.notapos.entity.MenuItemModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for MenuItemModifierGroup entity.
 * 
 * Provides database access methods for managing menu item and modifier group relationships.
 * 
 * @author CJ
 */

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, Long> {

    List<MenuItemModifierGroup> findByMenuItemId(Long menuItemId);                      // Find all modifier groups for a specific menu item

    List<MenuItemModifierGroup> findByModifierGroupId(Long modifierGroupId);            // Find all menu items that use a specific modifier group
}
