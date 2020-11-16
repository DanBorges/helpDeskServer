package dani.daniel.helpDesk.api.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import dani.daniel.helpDesk.api.User;

@Component
public interface UserService {
	
	User findByEmail (String email);
	
	User createOrUpdadte(User user);
	
	Optional<User> findById(String id);
	
	void delete (String id);
	
	Page<User> findAll(int page, int count);

}
