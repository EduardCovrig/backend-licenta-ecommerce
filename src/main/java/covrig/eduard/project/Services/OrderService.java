package covrig.eduard.project.Services;

import covrig.eduard.project.Models.*;
import covrig.eduard.project.Repositories.*;
import covrig.eduard.project.dtos.order.OrderResponseDTO;
import covrig.eduard.project.dtos.order.PlaceOrderDTO;
import covrig.eduard.project.mappers.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final OrderMapper orderMapper;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, ProductRepository productRepository, UserRepository userRepository, AddressRepository addressRepository, OrderMapper orderMapper, ProductService productService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.orderMapper = orderMapper;
        this.productService = productService;
    }

    //1. PLACE ORDER
    public OrderResponseDTO placeOrder(String userEmail, PlaceOrderDTO orderDTO)
    {
        User user=userRepository.findByEmail(userEmail).
                orElseThrow(() -> new RuntimeException("Nu exista user-ul cu email-ul " + userEmail));
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu are un cos."));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cosul este gol. Nu poti plasa o comanda.");
        }
        Address address = addressRepository.findById(orderDTO.getAddressId()) //verifica daca are o adresa utilizaotrul
                .orElseThrow(() -> new RuntimeException("Adresa invalida"));

        if (!address.getUser().getId().equals(user.getId())) { //daca adresa nu e a lui (bug extern ceva)
            throw new RuntimeException("Aceasta adresa nu iti apartine!");
        }

        //pasul de pregatire comanda
        Order order = new Order();
        //setare date comanda
        order.setUser(user);
        order.setCreatedAt(Instant.now());
        order.setStatus("PLACED");

        //adaugam produsele din cos in comanda
        order.setItems(new ArrayList<>());
        double totalOrderPrice = 0d;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int qtyToBuy = cartItem.getQuantity();

            if (product.getStockQuantity() < qtyToBuy) {
                throw new RuntimeException("Stoc insuficient pentru: " + product.getName());
            }

            //CALCUL PREȚ DINAMIC PE LOTURI
            Double itemSubtotal = productService.calculateSubtotalForQuantity(product, qtyToBuy);
            Double effectiveUnitPrice = itemSubtotal / qtyToBuy; // Preț mediu per unitate

            // --- ACTUALIZARE STOCURI (Prioritizam lotul care expira)
            int takenFromNearExpiry = Math.min(qtyToBuy, product.getNearExpiryQuantity());
            product.setNearExpiryQuantity(product.getNearExpiryQuantity() - takenFromNearExpiry);
            product.setStockQuantity(product.getStockQuantity() - qtyToBuy);
            productRepository.save(product);

            // Creare OrderItem
            OrderItem orderItem = orderMapper.cartItemToOrderItem(cartItem);
            orderItem.setOrder(order);
            orderItem.setPrice(effectiveUnitPrice); // Salvam pretul mediu platit
            orderItem.setBasePrice(product.getPrice());

            totalOrderPrice += itemSubtotal;
            order.getItems().add(orderItem);
        }
        //dupa ce trece prin fiecare item
        Order savedOrder = orderRepository.save(order);
        //salvam comanda in baza de date, savedOrder va avea si id-ul din baza de date preluat
        cart.getItems().clear();
        cartRepository.save(cart); //golim cosul
        return orderMapper.toDto(savedOrder); //returnam json cu OrderDto.
    }

    //Istoric comenzi utilizatof
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        List<Order> orders = orderRepository.findAllByUserId(user.getId());
        return orderMapper.toDtoList(orders);
    }

}
