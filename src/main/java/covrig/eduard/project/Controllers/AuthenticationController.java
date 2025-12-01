package covrig.eduard.project.Controllers;

import covrig.eduard.project.Security.AuthenticationService;
import covrig.eduard.project.dtos.auth.AuthenticationRequest;
import covrig.eduard.project.dtos.auth.AuthenticationResponse;
import covrig.eduard.project.dtos.user.UserRegistrationDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthenticationController {
    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Valid UserRegistrationDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        //returneaza 200 OK cu token-ul
        return ResponseEntity.ok(service.authenticate(request));
    }
}
