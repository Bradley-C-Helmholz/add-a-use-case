package team.projectpulse.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<PeerEvaluationUser, Integer> {

    Optional<PeerEvaluationUser> findByUsername(String username);

    Optional<PeerEvaluationUser> findByEmail(String email);

    List<PeerEvaluationUser> findByEmailIn(Collection<String> emails);

    /**
     * Whether the account is enabled, by primary key, for the per-request account-state check
     * (FR-SEC-active-account). Empty when no such user exists. Reads one column rather than the entity, so the
     * check stays one indexed lookup on every authenticated request.
     */
    @Query("select u.enabled from PeerEvaluationUser u where u.id = :userId")
    Optional<Boolean> isEnabled(@Param("userId") Integer userId);

}
