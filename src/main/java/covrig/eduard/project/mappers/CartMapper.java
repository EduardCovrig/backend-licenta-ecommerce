package covrig.eduard.project.mappers;

import covrig.eduard.project.Models.Cart;
import covrig.eduard.project.Models.CartItem;
import covrig.eduard.project.Services.ProductService;
import covrig.eduard.project.dtos.cart.CartItemResponseDTO;
import covrig.eduard.project.dtos.cart.CartResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CartMapper {
    @Autowired
    protected ProductService productService; //injectat pentru calculul dinamic
    // Mapare cart -> dto
    @Mapping(source="user.id",target = "userId") //(userId=cart.getUser()).getId(), iar userId este in CartResponseDTO
    @Mapping(target = "totalPrice", expression = "java(calculateTotalPrice(cart))")
    public abstract CartResponseDTO toDto(Cart cart);


    //Mapare CartItem -> dto
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.unitOfMeasure", target = "productUnit")
    @Mapping(target = "pricePerUnit", expression = "java(calculateEffectivePrice(item))")
    @Mapping(target = "subTotal", expression = "java(calculateSubTotal(item))")
   public abstract CartItemResponseDTO toItemDto(CartItem item);
    //metoda asta se foloseste automat de catre spring in toDto de mai sus ca sa transforme fiecare cartItem in CartItemResponseDTO
    //pentru lista de iteme ( spring cauta automat o metoda in mapper de forma "CartItemresponse DTO ... (CartItem ...)"




    //metodele pentru calculele din expresii
    public Double calculateSubTotal(CartItem item) {
        if (item.getProduct() == null) return 0d;
        return item.getProduct().getPrice() * item.getQuantity();
    }

    protected Double calculateEffectivePrice(CartItem item) {
        if (item.getProduct() == null || item.getQuantity() == 0) return 0.0;
        return calculateSubTotal(item) / item.getQuantity();
    }

    public Double calculateTotalPrice(Cart cart) {
        if (cart.getItems() == null) return 0d;
        return cart.getItems().stream()
                .mapToDouble(this::calculateSubTotal)
                .sum();
    }
}
