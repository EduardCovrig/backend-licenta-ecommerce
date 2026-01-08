package covrig.eduard.project.Services;

import covrig.eduard.project.Models.Product;
import covrig.eduard.project.Models.User;
import covrig.eduard.project.Models.UserInteraction;
import covrig.eduard.project.Repositories.ProductRepository;
import covrig.eduard.project.Repositories.UserInteractionRepository;
import covrig.eduard.project.Repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
@Transactional
public class UserInteractionService {
    private final UserInteractionRepository userInteractionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void logInteraction(String email, Long productId, String type)
    {
        User user=userRepository.findByEmail(email).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if(user!=null&&product!=null)
        {
            UserInteraction interaction=new UserInteraction();
            interaction.setUser(user);
            interaction.setInteractionType(type);
            interaction.setProduct(product); // 'VIEW', 'ADD_TO_CART', 'PURCHASE'
            interaction.setCreatedAt(Instant.now());
            userInteractionRepository.save(interaction);
        }
    }
}
