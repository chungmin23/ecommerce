package org.shop.apiserver.domain.model.cart;

import jakarta.persistence.*;
import lombok.*;
import org.shop.apiserver.domain.model.member.Member;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(exclude = "owner")
@Table(
  name = "tbl_cart", 
  indexes = { @Index(name="idx_cart_email", columnList = "member_owner") }
)
public class Cart {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cno;

  @OneToOne
  @JoinColumn(name="member_owner")
  private Member owner;

}
