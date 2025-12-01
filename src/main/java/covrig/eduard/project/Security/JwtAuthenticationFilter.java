package covrig.eduard.project.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//aceasta clasa se va apela la fiecare request primit de server
@Component
@RequiredArgsConstructor //face constructor cu toate campurile FINAL
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization"); //cauta headerul de autorizare
        final String jwt; //final in java se aplica odata ce i s-a atribuit o valoare, deci nu ramane final null
        final String userEmail; // la fel si aici, devine final odata cu prima atribuire.
        //le-am facut final ca sa nu le modificam din greseala in viitor.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {//daca nu l-a gasit, sau nu respecta formatul Bearer KEY, ignoram
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7); //daca l-a gasit, ajunge aici si extrage tot ce se afla dupa "BEARER " (adica jwt)
        userEmail = jwtService.extractUsername(jwt); //extrage email-ul din jwt, daca jwt nu e valid arunca SignatureException
        //(daca  jwt a fost generate cu alt secret_key)
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            //daca a gasit emailul si userul nu e deja autentificat
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail); //preluam datele utilizatorului din baza de date
            if (jwtService.isTokenValid(jwt, userDetails)) { //validam tokenul jwt
                //(se uita la data de expriare, daca e expirata returneaza false, si verifica daca emailul din jwt corespunde cu cel
                //din baza de cate)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(  //asta e ca o legitimatie de intrare in cont
                        //, fara ea, nu ai cum sa intri niciodata.
                        userDetails, //datele utilizatorului
                        null, //parola e considerata null ca a intrat cu tokenul
                        userDetails.getAuthorities() //ce drepturi are utilizatorul
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request) //detalii de locatie (de unde vine cererea)
                        //pune in legitimatie ip-ul.
                );
                SecurityContextHolder.getContext().setAuthentication(authToken); //setam userul ca fiind LOGAT.
                //practic ii ofera legitimatia utilizaotului, si cu ea va intra peste tot pe site, deci Spring va sti ca utilizatorul
                //este logat.
            }

        }
        filterChain.doFilter(request, response); //ca next() din js.
        //daca asta e ultimul filter, trimite mai departe request-ul direct catre controller, daca nu, la urmatorul filtru.
        //daca undeva pana aici pica, ca a aprut vreo exceptie deci utilizaotrul nu e in reugla, pur si simplu nu ajunge
        //aici niciodata, deci nu va trece la urmatorul filtru/controller -> FAIL.
    }
}
