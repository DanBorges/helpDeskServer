package dani.daniel.helpDesk.api.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import dani.daniel.helpDesk.api.ChangeStatus;
import dani.daniel.helpDesk.api.Ticket;
import dani.daniel.helpDesk.api.User;
import dani.daniel.helpDesk.api.dto.Summary;
import dani.daniel.helpDesk.api.enums.ProfileEnum;
import dani.daniel.helpDesk.api.enums.StatusEnum;
import dani.daniel.helpDesk.api.response.Response;
import dani.daniel.helpDesk.api.security.jwt.JwtTokenUtil;
import dani.daniel.helpDesk.api.service.TicketService;
import dani.daniel.helpDesk.api.service.UserService;

@RestController
@RequestMapping("api/ticket")
@CrossOrigin(origins = "*")
public class TicketController {
	
	@Autowired
	private TicketService ticketService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@PostMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> create(HttpServletRequest request,
													@RequestBody Ticket ticket,
													BindingResult result) {
		Response<Ticket> response = new Response<>();
		try {
			validateCreateTicket(ticket, result);
			if(result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			ticket.setStatus(StatusEnum.getStatus("New"));
			ticket.setUser(userFromRequest(request));
			ticket.setDate(new Date());
			ticket.setNumber(generateNumber());
			Ticket ticketPersisted = ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
		
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> update(HttpServletRequest request,
													@RequestBody Ticket ticket,
													BindingResult result) {
		
		Response<Ticket> response = new Response<Ticket>();
		
		try {
			validateUpdateTicket(ticket, result);
			if(result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			
			Optional<Ticket> ticketCurrent = ticketService.findById(ticket.getId());
			ticket.setStatus(ticketCurrent.get().getStatus());
			ticket.setUser(ticketCurrent.get().getUser());
			ticket.setDate(ticketCurrent.get().getDate());
			ticket.setNumber(ticketCurrent.get().getNumber());
			
			if(ticketCurrent.get().getAssignedUser() != null) {
				ticket.setAssignedUser(ticketCurrent.get().getAssignedUser());
			}
			Ticket ticketPersisted = ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);

	}
	
	
	@GetMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Ticket>> findById(@PathVariable("id") String id) {
		Response<Ticket> response = new Response<Ticket>();

		Optional<Ticket> ticket = ticketService.findById(id);
		if(ticket.get() == null) {
			response.getErrors().add("Register not found id: " + id);
			return ResponseEntity.badRequest().body(response);
		}
		List<ChangeStatus> changes =  new ArrayList<ChangeStatus>();
		Iterable<ChangeStatus> changesCurrent =  ticketService.listChangeStatus(ticket.get().getId());
		for(Iterator<ChangeStatus> iterator =  changes.iterator(); iterator.hasNext();) {
			ChangeStatus changeStatus = iterator.next();
			changeStatus.setTicket(null);
			changes.add(changeStatus);
		}
		ticket.get().setChanges(changes);
		response.setData(ticket.get());
		
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
		Response<String> response = new Response<String>();
		Optional<Ticket> ticket = ticketService.findById(id);
		if (ticket.get() == null) {
			response.getErrors().add("Register not found" + id);
			return ResponseEntity.badRequest().body(response);
		}
		ticketService.delete(id);
		return ResponseEntity.ok(response);
		
	}
	
	@GetMapping(value = "{page}/{count}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findAll(HttpServletRequest request, 
													@PathVariable int page,
													@PathVariable int count) {
		
		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		User userRequest = userFromRequest(request);
		if(userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {
			tickets = ticketService.listTicket(page, count);
		} else if(userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
			tickets = ticketService.findByCurrentUser(page, count, userRequest.getId());
		}
		response.setData(tickets);
		
		return ResponseEntity.ok(response);
		
	}
	
	@GetMapping("{page}/{count}/{number}/{title}/{status}/{priority}/{assigned}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findByParams(HttpServletRequest request,
																@PathVariable("page") int page,
																@PathVariable("count") int count,
																@PathVariable("number") Integer number,
																@PathVariable("title") String title,
																@PathVariable("status") String status,
																@PathVariable("priority") String priority,
																@PathVariable("assigned") boolean assigned) {
		
		title = title.equals("uninformed") ? null : title;
		status = status.equals("uninformed") ?  null : status;
		priority = status.equals("uninformed") ? null : priority;
		
		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		if(number > 0) {
			tickets = ticketService.findByNumber(page, count, number);
		} else {
			User userRequest = userFromRequest(request);
			if(userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {
				if(assigned) {
					tickets = ticketService.findByParametersAndAssignedUser(page, count, title, status, priority, userRequest.getId());
				} else {
					tickets = ticketService.findByParameters(page, count, title, status, priority);
				}
			} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
				tickets = ticketService.findByParametersAndCurrentUser(page, count, title, status, priority, userRequest.getId());
			}
		}
		response.setData(tickets);
		return ResponseEntity.ok(response);
		
	}
	
	@PutMapping("{id}/{status}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Ticket>> changeStatus(HttpServletRequest request,
														 @PathVariable("id") String id,
														 @PathVariable("status") String status,
														 @RequestBody Ticket ticket,
														 BindingResult result) {
		Response<Ticket> response = new Response<Ticket>();
		validateChangeStatus(id, status, result);
		if(result.hasErrors()) {
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}
		Optional<Ticket> ticketCurrent = ticketService.findById(id);
		ticketCurrent.get().setStatus(StatusEnum.getStatus(status));
		if(status.equals("Assigned")) {
			ticketCurrent.get().setAssignedUser(userFromRequest(request));
		}
		Ticket ticketPersisted = ticketService.createOrUpdate(ticketCurrent.get());
		ChangeStatus changeStatus = new ChangeStatus();
		changeStatus.setUserChange(userFromRequest(request));
		changeStatus.setDateChangeStatus(new Date());
		changeStatus.setStatus(StatusEnum.getStatus(status));
		changeStatus.setTicket(ticketPersisted);
		ticketService.createChangeStatus(changeStatus);
		response.setData(ticketPersisted);
		return ResponseEntity.ok(response);

	}
	
	@GetMapping(value = "/summary")
	public ResponseEntity<Response<Summary>> findSummary() {
		Response<Summary> response = new Response<>();
		Summary summary = new Summary();
		Integer amountNew = 0;
		Integer amountResolved = 0;
		Integer amountApproved = 0;
		Integer amountDisapproved = 0;
		Integer amountAssigned = 0;
		Integer amountClosed = 0;
		
		Iterable<Ticket> tickets = ticketService.findAll();
		if(tickets != null) {
			for(Iterator<Ticket> iterator = tickets.iterator(); iterator.hasNext();) {
				Ticket ticket = iterator.next();
				if(ticket.getStatus().equals(StatusEnum.New)) {
					amountNew++;
				}if(ticket.getStatus().equals(StatusEnum.Resolved)) {
					amountResolved++;
				}if(ticket.getStatus().equals(StatusEnum.Approved)) {
					amountApproved++;
				}if(ticket.getStatus().equals(StatusEnum.Disapproved)) {
					amountDisapproved++;
				}if(ticket.getStatus().equals(StatusEnum.Assigned)) {
					amountAssigned++;
				}if(ticket.getStatus().equals(StatusEnum.Closed)) {
					amountClosed++;
				}
			}
			summary.setAmountNew(amountNew);
			summary.setAmountResolved(amountResolved);
			summary.setAmountApproved(amountApproved);
			summary.setAmountAssigned(amountAssigned);
			summary.setAmountDisapproved(amountDisapproved);
			summary.setAmountClosed(amountClosed);
			response.setData(summary);
		}
		return ResponseEntity.ok(response);
		
	}
	
	
	private void validateChangeStatus(String id, String status, BindingResult result) {
		if (id == null || id.equals("")) {
			result.addError(new ObjectError("Ticket", "Id no information"));
			return ;
		}
		if (status == null || status.equals("")) {
			result.addError(new ObjectError("Ticket", "Status no information"));
			return ;
		}

	}
																
	
	private void validateCreateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title no information"));
			return ;
		}

	}
	
	private void validateUpdateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getId() == null) {
			result.addError(new ObjectError("Ticket", "Id no information"));
			return ;
		}
		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title no information"));
			return ;
		}

	}
	
	private User userFromRequest (HttpServletRequest request) {
		String token = request.getHeader("Authorization");
		String email =  jwtTokenUtil.getUsernameFromToken(token);
		return userService.findByEmail(email);

	}
	
	private Integer generateNumber() {
		Random random = new Random();
		return random.nextInt(9999);

	}

}
