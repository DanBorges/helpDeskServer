package dani.daniel.helpDesk.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import dani.daniel.helpDesk.api.ChangeStatus;

public interface ChangeStatusRepository extends MongoRepository<ChangeStatus, String> {

	Iterable<ChangeStatus> findByTicketIdOrderByDateChangeStatusDesc(String ticketId);
}