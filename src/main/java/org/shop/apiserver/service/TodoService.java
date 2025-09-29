package org.shop.apiserver.service;


import org.shop.apiserver.dto.PageRequestDTO;
import org.shop.apiserver.dto.PageResponseDTO;
import org.shop.apiserver.dto.TodoDTO;

public interface TodoService {
  
  Long register(TodoDTO todoDTO);

  TodoDTO get(Long tno);

  void modify(TodoDTO todoDTO);

  void remove(Long tno);

  PageResponseDTO<TodoDTO> list(PageRequestDTO pageRequestDTO);

}
