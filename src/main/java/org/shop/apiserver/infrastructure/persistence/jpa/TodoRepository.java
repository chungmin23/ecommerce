package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.todo.Todo;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TodoRepository extends JpaRepository<Todo, Long>{
  
}
