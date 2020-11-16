package dani.daniel.helpDesk.api.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import dani.daniel.helpDesk.api.User;
import dani.daniel.helpDesk.api.repository.UserRepository;
import dani.daniel.helpDesk.api.service.UserService;

@Component
public class UserServiceImpl implements UserService {
	
	@Autowired
	UserRepository userRepository;

	@Override
	public User findByEmail(String email) {
		return this.userRepository.findByEmail(email);
	}

	@Override
	public User createOrUpdadte(User user) {
		return this.userRepository.save(user);
	}

	@Override
	public Optional<User> findById(String id) {
		return this.userRepository.findById(id);
	}

	@Override
	public void delete(String id) {
		this.userRepository.deleteById(id);
		
	}

	@Override
	public Page<User> findAll(int page, int count) {
		Pageable pages =  PageRequest.of(page, count);
		return this.userRepository.findAll(pages);
	}

}
