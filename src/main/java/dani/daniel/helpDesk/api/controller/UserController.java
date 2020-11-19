package dani.daniel.helpDesk.api.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dani.daniel.helpDesk.api.User;
import dani.daniel.helpDesk.api.response.Response;
import dani.daniel.helpDesk.api.service.UserService;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<Response<User>> create(HttpServletRequest request, 
												@RequestBody User user,
												BindingResult result) {
		Response<User> response = new Response<User>();
		try {
			validadeCreateUser(user, result);
			if(result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			User userPersisted = userService.createOrUpdadte(user);
			response.setData(userPersisted);
		} catch (DuplicateKeyException e) {
			response.getErrors().add("Email already exist");
			return ResponseEntity.badRequest().body(response);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}
	
	
	private void validadeCreateUser(User user, BindingResult result) {
		if(user.getEmail() == null) {
			result.addError(new ObjectError("User", "Email no informaton"));
		}
		
	}
	
	@PutMapping
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<Response<User>> update(HttpServletRequest request, 
			@RequestBody User user,
			BindingResult result) {
		Response<User> response = new Response<>();
		try {
			validadteUpdateUser(user, result);
			if(result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			User userPersisted = userService.createOrUpdadte(user);
			response.setData(userPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
		}
		return ResponseEntity.ok(response);
	}
	
	private void validadteUpdateUser(User user, BindingResult result) {
		
		if (user.getId() == null) {
			result.addError(new ObjectError("User", "Id no information"));
		}
		if (user.getEmail() == null) {
			result.addError(new ObjectError("User", "Email no information"));
		}

	}
	
	@GetMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<Response<User>> findById(@PathVariable("id") String id) {
		Response<User> response = new Response<User>();
		Optional<User> user = userService.findById(id);
		if(user == null) {
			response.getErrors().add("Register Not Found" + id);
			return ResponseEntity.badRequest().body(response);
		}
		response.setData(user.get());
		return ResponseEntity.ok(response);

	}
	
	@DeleteMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
		Response<String> response = new Response<String>();
		Optional<User> user = userService.findById(id);
		if(user == null) {
			response.getErrors().add("Register Not Found" + id);
			return ResponseEntity.badRequest().body(response);
		}
		 userService.delete(id);
		 return ResponseEntity.ok(response);
		
	}
	
	@GetMapping(value = "{page}/{count}")
	public ResponseEntity<Response<Page<User>>> findAll (@PathVariable int page,
														@PathVariable int count) {
		Response<Page<User>> response = new Response<Page<User>>();
		Page<User> users = userService.findAll(page, count);
		response.setData(users);
		return ResponseEntity.ok(response);
		
	}

}
