package org.shop.apiserver.domain.model.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_product")
@Getter
@ToString(exclude = "imageList")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pno;

  private String pname;

  private int price;

  private String pdesc;

  private boolean delFlag;

  @Column(nullable = false)
  @Builder.Default
  private int stock = 0;


  public void changeDel(boolean delFlag) {
    this.delFlag = delFlag;
  }


  @ElementCollection
  @Builder.Default
  @BatchSize(size = 20)
  private List<ProductImage> imageList = new ArrayList<>();

  // 재고 감소 메서드
  public void decreaseStock(int quantity) {
    if (this.stock < quantity) {
      throw new IllegalStateException(
              String.format("재고 부족: 현재 %d개, 요청 %d개", this.stock, quantity)
      );
    }
    this.stock -= quantity;
  }

  // 재고 증가 메서드 (주문 취소 시)
  public void increaseStock(int quantity) {
    this.stock += quantity;
  }

  // 재고 설정 메서드
  public void changeStock(int stock) {
    this.stock = stock;
  }

  public void changePrice(int price) {
    this.price = price;
  }

  public void changeDesc(String desc){
      this.pdesc = desc;
  }

  public void changeName(String name){
      this.pname = name;
  }

  public void addImage(ProductImage image) {

      image.setOrd(this.imageList.size());
      imageList.add(image);
  }

  public void addImageString(String fileName){

    ProductImage productImage = ProductImage.builder()
    .fileName(fileName)
    .build();
    addImage(productImage);

  }

  public void clearList() {
      this.imageList.clear();
  }
}
