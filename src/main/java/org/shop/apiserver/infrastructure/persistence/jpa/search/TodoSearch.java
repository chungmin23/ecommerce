package org.shop.apiserver.infrastructure.persistence.jpa.search;

import org.shop.apiserver.domain.model.todo.Todo;
import org.springframework.data.domain.Page;

public interface TodoSearch {

    Page<Todo> search1();
}
