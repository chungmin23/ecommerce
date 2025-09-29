package org.shop.apiserver.repository.search;

import com.querydsl.jpa.JPQLQuery;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.QTodo;
import org.shop.apiserver.domain.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;


@Log4j2
public class TodoSearchImpl extends QuerydslRepositorySupport implements TodoSearch {

  public TodoSearchImpl() {
    super(Todo.class);
  }

    @Override
    public Page<Todo> search1() {

      log.info("search1............");

        QTodo todo = QTodo.todo;

        JPQLQuery<Todo> query = from(todo);

        query.where(todo.title.contains("1"));

        Pageable pageable = PageRequest.of(0,10);

        this.getQuerydsl().applyPagination(pageable, query);

        query.fetch();

        query.fetchCount();

      return null;
    }
}
