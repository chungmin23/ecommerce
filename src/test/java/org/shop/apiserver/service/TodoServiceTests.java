package org.shop.apiserver.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.shop.apiserver.application.service.TodoService;
import org.shop.apiserver.application.dto.PageRequestDTO;
import org.shop.apiserver.application.dto.PageResponseDTO;
import org.shop.apiserver.application.dto.TodoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
public class TodoServiceTests {

  @Autowired
  private TodoService todoService;

  @Test
  public void testRegister() {

    TodoDTO todoDTO = TodoDTO.builder()
    .title("서비스 테스트")
    .writer("tester")
    .dueDate(LocalDate.of(2023,10,10))
    .build();

    Long tno = todoService.register(todoDTO);

    log.info("TNO: " + tno);

  }

  @Test
  public void testGet() {

    Long tno = 101L;

    TodoDTO todoDTO = todoService.get(tno);

    log.info(todoDTO);

  }

  @Test
  public void testList() {

    PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
    .page(2)
    .size(10)
    .build();

    PageResponseDTO<TodoDTO> response = todoService.list(pageRequestDTO);

    log.info(response);

  }



}
