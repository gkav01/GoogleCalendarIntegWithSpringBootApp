package com.example.demo.dto;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor()
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EventResponseDTO {
	
	@NotBlank(message = "Event subject cannot be empty")
	private String eventId;
	
	@NotBlank(message = "Event description cannot be empty")
	private String eventDescription;
	
	@NotBlank(message = "Event summary cannot be empty")
	private String eventSummary;
	
	@NotBlank(message = "Event meeting link cannot be empty")
	private String  eventStatus;
	
	private String location;
	
	@NotBlank(message = "Event start date time cannot be empty")
	private String eventStartDateTime;
	
	@NotBlank(message ="Event end date time cannot be empty")
	private String eventEndDateTime;
	
}
