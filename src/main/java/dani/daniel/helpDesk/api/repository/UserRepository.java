package dani.daniel.helpDesk.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import dani.daniel.helpDesk.api.User;

public interface UserRepository extends MongoRepository<User, String> {
	User findByEmail(String email);

}
