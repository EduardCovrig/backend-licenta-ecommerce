package covrig.eduard.project.Services;

import covrig.eduard.project.Models.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    public void sendOrderConfirmation(String email, Order order) {
        // Poate implementez altadata JavaMailSender
        //momentan, facem doar log-uri in consola
        log.info("----------------------------------------------------------------");
        log.info("EMAIL SENT TO: {}", email);
        log.info("SUBJECT: Confirmare Comandă #{}", order.getId());
        log.info("BODY: Salut! Comanda ta în valoare de {} RON a fost confirmata.", order.getTotalPrice());
        log.info("----------------------------------------------------------------");
    }
    public void sendWelcomeEmail(String email) {
        log.info("EMAIL SENT TO: {}", email);
        log.info("SUBJECT: Bine ai venit pe platforma EdwC Store!");
    }
}
