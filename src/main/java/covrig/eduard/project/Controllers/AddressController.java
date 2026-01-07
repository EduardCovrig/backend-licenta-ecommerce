package covrig.eduard.project.Controllers;

import covrig.eduard.project.Services.AddressService;
import covrig.eduard.project.dtos.address.AddressCreationDTO;
import covrig.eduard.project.dtos.address.AddressResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponseDTO>> getUserAddresses(Authentication authentication)
    {
        return ResponseEntity.ok(addressService.getMyAddresses(authentication.getName()));
    }
    @GetMapping
    public ResponseEntity<AddressResponseDTO> getAddressById(@PathVariable Long id)
    {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }
    @PostMapping
    public ResponseEntity<AddressResponseDTO> createAddress(Authentication authentication, @RequestBody @Valid AddressCreationDTO dto)
    {
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(addressService.createAddress(authentication.getName(),dto));
    }
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> updateAddress(@PathVariable Long id, Authentication authentication, @RequestBody @Valid AddressCreationDTO dto)
    {
        return ResponseEntity.ok(addressService.updateAddress(id,authentication.getName(),dto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> deleteAddress(@PathVariable Long id, Authentication authentication)
    {
        return ResponseEntity.ok(addressService.deleteAddress(id, authentication.getName()));
    }

}
