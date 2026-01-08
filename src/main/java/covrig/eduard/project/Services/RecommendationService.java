package covrig.eduard.project.Services;

import covrig.eduard.project.Models.Product;
import covrig.eduard.project.Models.User;
import covrig.eduard.project.Models.UserInteraction;
import covrig.eduard.project.Repositories.ProductRepository;
import covrig.eduard.project.Repositories.UserInteractionRepository;
import covrig.eduard.project.Repositories.UserRepository;
import covrig.eduard.project.dtos.product.ProductResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final UserInteractionRepository interactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public RecommendationService(UserInteractionRepository interactionRepository, ProductRepository productRepository, UserRepository userRepository, ProductService productService) {
        this.interactionRepository = interactionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }
    public List<ProductResponseDTO> getRecommendations(String email)
    {
        if (email == null || email.equals("anonymousUser")) {
        return getGlobalTopProducts();
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return getGlobalTopProducts();

        //Preluam interactiunile utilizatorului existent
        // 1. Preluăm interacțiunile utilizatorului
        List<UserInteraction> interactions = interactionRepository.findAllByUserId(user.getId());

        if (interactions.isEmpty()) {
            return getGlobalTopProducts();
        }
        //2. identificam categoiile preferate
        Map<String,Long> categoryPreferences=interactions.stream().map(i->i.getProduct().getCategory().getName())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        //3. Top 2 categorii cele mai vizitate sau cumparate
        List<String> topCategories=categoryPreferences.entrySet().stream().
                sorted((e1,e2)-> e1.getValue().compareTo(e2.getValue()))
                .limit(2)
                .map(Map.Entry::getKey)//key e numele categoriei
                .collect(Collectors.toList());
        //4. Recomandarea efectiva a celor 2 categorii de top pentru utilizator
        List<Product> recommendedProducts=productRepository.findAll().stream()
                        .filter(p -> topCategories.contains(p.getCategory().getName()))
                .filter(p -> p.getStockQuantity()>0).limit(5)
                .collect(Collectors.toList());
        return recommendedProducts.stream().map(p -> productService.getProductById(p.getId()))
                //SE APLICA AUTOMAT ENRICHMENTUL DIN PRODUCTSERVICE prin apelarea getProductById
                .collect(Collectors.toList());

    }
    private List<ProductResponseDTO> getGlobalTopProducts() {
        //Metoda de fallback, returnam primele 5 produse din stoc (poate modificat pe viitor la cele mai vandute da nu am chef
        //sa fac asta acum)
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() > 0)
                .limit(5)
                .map(p -> productService.getProductById(p.getId()))
                .collect(Collectors.toList());
    }

}
