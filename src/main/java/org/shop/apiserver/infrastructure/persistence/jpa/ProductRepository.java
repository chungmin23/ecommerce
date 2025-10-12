package org.shop.apiserver.infrastructure.persistence.jpa;

import jakarta.persistence.LockModeType;
import org.shop.apiserver.domain.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>{

  // 비관적 락 추가
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.pno = :pno")
  Optional<Product> findByIdWithPessimisticLock(@Param("pno") Long pno);


  @EntityGraph(attributePaths = "imageList")
  @Query("select p from Product p where p.pno = :pno")
  Optional<Product> selectOne(@Param("pno") Long pno);

  @Modifying
  @Query("update Product p set p.delFlag = :flag where p.pno = :pno")
  void updateToDelete(@Param("pno") Long pno , @Param("flag") boolean flag);

  
  @Query("select p, pi  from Product p left join p.imageList pi  where pi.ord = 0 and p.delFlag = false ")
  Page<Object[]> selectList(Pageable pageable);


  @Query("select p, pi  from Product p left join p.imageList pi  where pi.ord >= 0 and p.delFlag = false ")
  Page<Object[]> selectList2(Pageable pageable);


  @Query("select p from Product p left join p.imageList pi  where p.delFlag = false ")
  Page<Product> selectListWitAll(Pageable pageable);
}
