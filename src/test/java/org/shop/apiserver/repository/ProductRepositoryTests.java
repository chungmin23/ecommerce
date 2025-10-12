package org.shop.apiserver.repository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;


import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Log4j2
public class ProductRepositoryTests {

  @Autowired
  ProductRepository productRepository;


  /**
   * 재고 테스트용 상품 생성
   */
  @Test
  @Transactional
  @Commit
  public void testInsertProductsWithStock() {

    for (int i = 1; i <= 10; i++) {
      Product product = Product.builder()
              .pname("재고테스트 상품 " + i)
              .price(10000 * i)
              .pdesc("동시성 테스트용 상품 " + i)
              .stock(100)  // ✅ 초기 재고 100개
              .delFlag(false)
              .build();

      product.addImageString("test_image_" + i + ".jpg");

      productRepository.save(product);
      log.info("✅ 상품 생성: {} - 재고: {}", product.getPname(), product.getStock());
    }

    log.info("========================================");
    log.info("테스트 상품 10개 생성 완료 (각 재고 100개)");
    log.info("========================================");
  }

  /**
   * 재고 초기화 (테스트 전)
   */
  @Test
  @Transactional
  @Commit
  public void testResetStock() {

    for (long i = 1; i <= 10; i++) {
      Product product = productRepository.findById(i).orElse(null);
      if (product != null) {
        product.changeStock(100);
        productRepository.save(product);
        log.info("재고 초기화: {} - 100개", product.getPname());
      }
    }

    log.info("========================================");
    log.info("모든 상품 재고 100개로 초기화 완료");
    log.info("========================================");
  }

//
//  @Test
//  public void testInsert() {
//
//    for (int i = 0; i < 10; i++) {
//
//      Product product = Product.builder()
//      .pname("상품"+i)
//      .price(100*i)
//      .pdesc("상품설명 " + i)
//      .build();
//
//      //2개의 이미지 파일 추가
//      product.addImageString("IMAGE1.jpg");
//      product.addImageString("IMAGE2.jpg");
//
//      productRepository.save(product);
//
//      log.info("-------------------");
//    }
//  }
//
//  @Transactional
//  @Test
//  public void testRead() {
//
//    Long pno = 1L;
//
//    Optional<Product> result = productRepository.findById(pno);
//
//    Product product = result.orElseThrow();
//
//    log.info(product); // --------- 1
//    log.info(product.getImageList()); // ---------------------2
//
//  }
//
//  @Test
//  public void testRead2() {
//
//    Long pno = 1L;
//
//    Optional<Product> result = productRepository.selectOne(pno);
//
//    Product product = result.orElseThrow();
//
//    log.info(product);
//    log.info(product.getImageList());
//
//  }
//
//  @Commit
//  @Transactional
//  @Test
//  public void testDelte() {
//
//    Long pno = 2L;
//
//    productRepository.updateToDelete(pno, true);
//
//  }
//
//  @Test
//  public void testUpdate(){
//
//    Long pno = 10L;
//
//    Product product = productRepository.selectOne(pno).get();
//
//    product.changeName("10번 상품");
//    product.changeDesc("10번 상품 설명입니다.");
//    product.changePrice(5000);
//
//    //첨부파일 수정
//    product.clearList();
//
//    product.addImageString(UUID.randomUUID().toString()+"_"+"NEWIMAGE1.jpg");
//    product.addImageString(UUID.randomUUID().toString()+"_"+"NEWIMAGE2.jpg");
//    product.addImageString(UUID.randomUUID().toString()+"_"+"NEWIMAGE3.jpg");
//
//    productRepository.save(product);
//
//  }
//
//  @Test
//  public void testList() {
//
//    //org.springframework.data.domain 패키지
//    Pageable pageable = PageRequest.of(0, 10, Sort.by("pno").descending());
//
//    Page<Object[]> result = productRepository.selectList(pageable);
//
//    //java.util
//    result.getContent().forEach(arr -> log.info(Arrays.toString(arr)));
//
//  }
//
//  @Test
//  public void testList2() {
//
//    //org.springframework.data.domain 패키지
//    Pageable pageable = PageRequest.of(0, 10, Sort.by("pno").descending());
//
//    Page<Object[]> result = productRepository.selectList2(pageable);
//
//    //java.util
//    result.getContent().forEach(arr -> log.info(Arrays.toString(arr)));
//
//  }
//
//  @Transactional
//  @Test
//  public void testListWithAll() {
//
//    //org.springframework.data.domain 패키지
//    Pageable pageable = PageRequest.of(0, 10, Sort.by("pno").descending());
//
//    Page<Product> result = productRepository.selectListWitAll(pageable);
//
//    //java.util
//    result.getContent().forEach(product ->{
//
//      log.info(product);
//      log.info(product.getImageList());
//
//    });
//
//  }



}
