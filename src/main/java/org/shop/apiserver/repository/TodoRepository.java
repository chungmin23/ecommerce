package org.shop.apiserver.repository;

import org.shop.apiserver.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TodoRepository extends JpaRepository<Todo, Long>{
  
}
