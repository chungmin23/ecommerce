package org.shop.apiserver.application.service;


import org.shop.apiserver.application.dto.PageRequestDTO;
import org.shop.apiserver.application.dto.PageResponseDTO;
import org.shop.apiserver.application.dto.TodoDTO;

public interface TodoService {
  
  Long register(TodoDTO todoDTO);

  TodoDTO get(Long tno);

  void modify(TodoDTO todoDTO);

  void remove(Long tno);

  PageResponseDTO<TodoDTO> list(PageRequestDTO pageRequestDTO);

}
