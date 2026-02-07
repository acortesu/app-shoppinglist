package com.appcompras.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShoppingListDraftRepository extends JpaRepository<ShoppingListDraftEntity, String> {

    List<ShoppingListDraftEntity> findAllByOrderByCreatedAtDescIdAsc();
}
