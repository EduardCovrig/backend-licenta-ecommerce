package covrig.eduard.project.Controllers;

import covrig.eduard.project.Services.OrderService;
import covrig.eduard.project.dtos.order.OrderResponseDTO;
import covrig.eduard.project.dtos.order.PlaceOrderDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 1. PLASARE comanda
    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(Authentication authentication, @RequestBody @Valid PlaceOrderDTO dto)
    {
        OrderResponseDTO order = orderService.placeOrder(authentication.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // 2. ISTORIC COMENZI
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(Authentication authentication)
    {
        return ResponseEntity.ok(orderService.getUserOrders(authentication.getName()));
    }

    // 3. DETALII O COMANDA dupa id
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id, Authentication authentication)
    {
        // Verificarea de securitate are loc in service
        return ResponseEntity.ok(orderService.getOrderById(id, authentication.getName()));
    }
}