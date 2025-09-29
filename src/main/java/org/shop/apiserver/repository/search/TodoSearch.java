package org.shop.apiserver.repository.search;

import org.shop.apiserver.domain.Todo;
import org.springframework.data.domain.Page;

public interface TodoSearch {

    Page<Todo> search1();
}
