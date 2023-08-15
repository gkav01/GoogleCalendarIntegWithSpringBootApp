package com.example.demo.controller;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EventDTO;
import com.example.demo.dto.EventResponseDTO;
import com.example.demo.service.GoogleCalendarService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@Api(tags = "Google Calendar Integration Controller")
public class GoogleCalendarController {

	@Autowired
	private GoogleCalendarService gCalendarService;
	
	@GetMapping(path = "/gcalendar/events", produces = "application/json")
	@ApiOperation("Gat all the events from the calendar")
	public ResponseEntity<List<EventResponseDTO>> getCalendarEvents() throws Exception
	{
		//Get Calendar events
		List<EventResponseDTO> caleResponse = gCalendarService.getEventsUsingServiceAcc();
		return new ResponseEntity<List<EventResponseDTO>>(caleResponse, HttpStatus.OK);
	}

	@PostMapping(path = "/gcalendar/event", produces = "application/json")
	@ApiOperation("Gat all the calendars from given startDate to endDate")
	@RolesAllowed({"ROLE_ADMIN"})
	public ResponseEntity<String> createCalendarEvent(@Valid @RequestBody EventDTO eventRequest) 
			throws Exception
	{
		gCalendarService.createEventUsingServiceAcc(eventRequest);
		return new ResponseEntity<String>("Event creation success", HttpStatus.CREATED);
	}
	
	//Event update is currently not working 
	@PutMapping(path = "/gcalendar/event/{id}", produces = "application/json")
	@ApiOperation("Gat all the calendars from given startDate to endDate")
	@RolesAllowed({"ROLE_ADMIN"})
	public ResponseEntity<String> updateCalendarEvent(@PathVariable("id")String eventId, @Valid @RequestBody EventDTO eventRequest) 
			throws Exception
	{
			gCalendarService.updateEvent(eventId, eventRequest);
			return new ResponseEntity<String>("Event update success", HttpStatus.OK);
	}
	
	@DeleteMapping(path = "/gcalendar/event/{id}")
	@ApiOperation("Deletes the event with the given event ID")
	@RolesAllowed({"ROLE_ADMIN"})
	public ResponseEntity<String> deleteCalendarEvent(@PathVariable ("id") String eventId) 
			throws Exception
	{
			gCalendarService.deleteEvent(eventId);
			return new ResponseEntity<String>("Event deletion success", HttpStatus.OK);
	}
}
