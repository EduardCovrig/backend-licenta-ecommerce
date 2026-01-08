package covrig.eduard.project.Security;


import covrig.eduard.project.Repositories.UserRepository;
import covrig.eduard.project.Services.NotificationService;
import covrig.eduard.project.Services.UserService;
import covrig.eduard.project.dtos.auth.AuthenticationRequest;
import covrig.eduard.project.dtos.auth.AuthenticationResponse;
import covrig.eduard.project.dtos.user.UserRegistrationDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.management.Notification;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService; //pentru a refolosi logica de creare user, atat.
    private final NotificationService notificationService;

    public AuthenticationService(UserRepository userRepository, JwtService jwtService, AuthenticationManager authenticationManager, UserService userService, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    //1. METODA DE REGISTER (CREEAZA USER-UL SI RETURNEAZA TOKEN-UL ACESTUIA)
    public AuthenticationResponse register(UserRegistrationDTO req)
    {
        userService.createUser(req); //salvam user-ul in baza de date.
        var user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(); //gasim user-ul creat in baza de date.
        //realistic, nu os a intre niciodata pe orelsethrow ca nu are de ce, user-ul abia fiind creat, deci nu are rost
        //sa punem o exceptie custom, totusi, daca chiar intra, va fi prinsa de runtimeexception si va returna ceva generic
        //dar nu se va intampla abasolut niciodat asta.
        var jwtToken = jwtService.generateToken(user); //ii generam token jwt
        notificationService.sendWelcomeEmail(req.getEmail());

        return new AuthenticationResponse(jwtToken); //returnam tokenul.
    }

    // 2. METODA DE LOGIN (VERIFICA PAROLA SI RETURNEAZA TOKEN-UL)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
       //verifica email-ul si parola criptata
       //daca parola e gresita, arunca exceptie 403 Forbidden automat.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Dacă am ajuns aici, userul e valid. Il cautam si ii dam token-ul.
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found")); //mi-l cauta im baza de date
        //daca nu il gaseste, arunca exceptie

        var jwtToken = jwtService.generateToken(user); //ii generam un token

        return new AuthenticationResponse(jwtToken); //il returnam, nu il stocam nicaieri
        //mai departe, e treaba frontend-ului sa il gestioneze pentru vitioarele cereri
    }
}
