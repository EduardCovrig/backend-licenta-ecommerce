package covrig.eduard.project.Controllers;

import covrig.eduard.project.Services.ProductService;
import covrig.eduard.project.Services.UserInteractionService;
import covrig.eduard.project.dtos.product.ProductCreationDTO;
import covrig.eduard.project.dtos.product.ProductResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController()
@RequestMapping("/api/products")

//VALID se pune doar la @RequestBody, deoarece pe get sau pe pathvariable datele sunt deja valide 100%.
//(Fie provin din baza de date, fie respecta tipul Long)
public class ProductController {

    final private ProductService productService;
    final private UserInteractionService interactionService;

    public ProductController(ProductService productService, UserInteractionService interactionService) {
        this.productService = productService;
        this.interactionService = interactionService;
    }
    //OPERATII DE GET

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts()
    {
        return ResponseEntity.ok(productService.getAllProducts()); //200 OK
        // echivalent cu new ResponseEntity<>(...,HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id, Authentication authentication)
    {
        if (authentication != null) {
            interactionService.logInteraction(authentication.getName(), id, "VIEW"); //adaugam interactiunea userului,
            //daca este logat, in tabela lui de interactiuni
        }
        return ResponseEntity.ok(productService.getProductById(id));
    }

    //GET /api/products/expiring?date=2025-01-01
    @GetMapping("/expiring")
    public ResponseEntity<List<ProductResponseDTO>> getProductsExpiringBefore(@RequestParam LocalDate date)
    {
        List<ProductResponseDTO> products=productService.getProductsExpiringBefore(date);
        return ResponseEntity.ok(products); //200 OK
    }

    @GetMapping("/filter")  //metode de filtrare, se pot adauga ulterior mai multe @RequestParam cu
    //required=false, si crearea de if-uri pentru gestionare. Momentan, se poate filtra doar dupa o singura categorie
    // mai exact prima in ordinea verificata in if-uri. Pentru a avea mai multe filtre deodata, e nevoie de o metoda
    //noua in repository care sa le combine intr-un if cu doua verificari de null, pusa prima.
    public ResponseEntity<List<ProductResponseDTO>> getProductsByFilter(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category) {

        if (brand != null) {
            return ResponseEntity.ok(productService.getProductsByBrandName(brand));
        }

        if (category != null) {
            return ResponseEntity.ok(productService.getProductsByCategoryName(category));
        }

        // Daca nu e niciun filtr, returnam toate prdusele cu statusul 200 OK
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // WRITE OPERATIONS (POST,PUT,DELETE)

    //POST /api/products
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody @Valid ProductCreationDTO creationDTO)
    {
        ProductResponseDTO newProduct=productService.createProduct(creationDTO);
        return new ResponseEntity<>(newProduct,HttpStatus.CREATED);
        // echivalent cu return ResponseEntity.status(Httpstatus.CREATED).body(newProduct);
    }

    //PUT /api/products/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id, @RequestBody @Valid ProductCreationDTO updateDTO)
    {
        ProductResponseDTO updatedProduct= productService.updateProduct(id,updateDTO);
        return ResponseEntity.ok(updatedProduct); //200 OK
    }

    //DELETE /api/products/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> deleteProduct(@PathVariable Long id)
    {
        ProductResponseDTO deletedProduct= productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.OK).body(deletedProduct);
        //returnaza statusul 200 OK si produsl sters ca raspuns de confirmare
    }




}

