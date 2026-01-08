package covrig.eduard.project.Repositories;

import covrig.eduard.project.Models.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInteractionRepository extends JpaRepository<UserInteraction,Long> {
    List<UserInteraction> findAllByUserId(Long userId);
}
