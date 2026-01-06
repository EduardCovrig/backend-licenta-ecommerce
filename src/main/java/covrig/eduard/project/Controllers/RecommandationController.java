package covrig.eduard.project.Controllers;

import covrig.eduard.project.Services.RecommendationService;
import covrig.eduard.project.dtos.product.ProductResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin("*")

public class RecommandationController {
    private final RecommendationService recommendationService;

    public RecommandationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getMyRecommendations(Authentication authentication)
    {
        String email=(authentication!=null) ? authentication.getName() : null;
        return ResponseEntity.ok(recommendationService.getRecommendations(email));
    }
}
