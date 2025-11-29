package com.bandhanbook.app.repository;

import com.bandhanbook.app.model.MatrimonyCandidate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatrimonyRepository extends MongoRepository<MatrimonyCandidate, String> {
    Optional<List<MatrimonyCandidate>> getCandidatesByUserId(String user_id);
}
