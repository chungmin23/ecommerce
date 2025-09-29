package org.shop.apiserver.service;

import org.shop.apiserver.dto.PageRequestDTO;
import org.shop.apiserver.dto.PageResponseDTO;
import org.shop.apiserver.dto.ProductDTO;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public interface ProductService {

  PageResponseDTO<ProductDTO> getList(PageRequestDTO pageRequestDTO);

  Long register(ProductDTO productDTO);

  ProductDTO get(Long pno);

  void modify(ProductDTO productDTO);

  void remove(Long pno);

}
