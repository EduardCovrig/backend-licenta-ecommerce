package covrig.eduard.project.dtos.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Data // Include @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreationDTO {

    // 1. Campuri de identificare (fara id)
    @NotBlank(message = "Numele produsului este obligatoriu.") // NOT BLANK => NOT NULL SAU "" SAU DOAR SPATII
    // SE FOLOSESTE DE OBICEI LA STRINGURI
    private String name;


    // 2. Campuri necesare pentru pret si stoc

    // Pretul trebuie sa existe si sa fie pozitiv
    @NotNull(message = "Pretul este obligatoriu.")
    @Positive(message = "Pretul trebuie sa fie o valoare pozitiva.")
    private Double price;

    // Stocul trebuie sa existe si sa fie zero sau pozitiv
    @NotNull(message = "Cantitatea initiala în stoc este obligatorie.")
    @PositiveOrZero(message = "Stocul nu poate fi negativ.")
    private Integer stockQuantity;

    // Unitatea de masura trebuie sa existe
    @NotBlank(message = "Unitatea de masura (ex: kg, buc, L) este obligatorie.")
    private String unitOfMeasure;

    // 3. Campuri specifice produselor alimentare
    //fara @NotNull, nu toate produsele au termen de expirare strict.
    private LocalDate expirationDate;

    // 4. Relatii (Chei Straine - FK)

    // ID-urile FK trebuie sa existe
    @NotNull(message = "ID-ul Brand-ului este obligatoriu.")
    private Long brandId;

    @NotNull(message = "ID-ul Categoriei este obligatoriu.")
    private Long categoryId;

    private List<String> imageUrls;
    private Map<String, String> attributes;
}