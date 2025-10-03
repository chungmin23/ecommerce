package org.shop.apiserver.domain.model.product;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {

    private String fileName; 

    private int ord;

    public void setOrd(int ord){
        this.ord = ord;
    }

}
