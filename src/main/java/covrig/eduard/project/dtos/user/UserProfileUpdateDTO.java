package covrig.eduard.project.dtos.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor@AllArgsConstructor
public class UserProfileUpdateDTO {
    //permite modifiarea doar pentru campurile urmatoare:
    @NotBlank(message = "Prenumele este obligatoriu.")
    private String firstName;
    @NotBlank(message = "Prenumele este obligatoriu.")
    private String lastName;
    private String phoneNumber;
    private String password;
}
