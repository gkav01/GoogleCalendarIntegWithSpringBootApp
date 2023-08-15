package com.example.demo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.dto.EventDTO;
import com.example.demo.dto.EventResponseDTO;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.Calendar.Events;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.numpyninja.lms.dto.gcalendar.GCalendarEventRequestDTO;
import com.numpyninja.lms.dto.gcalendar.GCalendarEventResponseDTO;
import com.numpyninja.lms.exception.CalendarAccessDeniedException;
import com.numpyninja.lms.exception.GCalendarCreateEventException;
import com.numpyninja.lms.exception.GCalendarDeleteEventException;
import com.numpyninja.lms.exception.GCalendarEventNotFoundException;
import com.numpyninja.lms.exception.GCalendarIOException;
import com.numpyninja.lms.exception.GCalendarSecurityException;
import com.numpyninja.lms.mappers.GCalendarEventsMapper;
import com.numpyninja.lms.services.KeyService;

@Service
public class GoogleCalendarService {

	@Value("${spring.application.name}")
	private String APPLICATION_NAME;

	@Value("${google.calendar-id-lms}")
	private String CALENDAR_ID;
	
	
	String credentialFile="credentials.json";
	private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

	//Load credentials from file to GoogleCredentials Object
	private GoogleCredentials getServiceCredentials() throws Exception {
		GoogleCredentials credential;
		try {
			credential = GoogleCredentials
					.fromStream(new FileInputStream(new File(credentialFile)))
					.createScoped(Collections.singletonList(CalendarScopes.CALENDAR));
			
		} catch (Exception e) {
			logger.error("Error: ", e);
			throw new Exception(e.getLocalizedMessage());
		}
		// .createDelegated("numpyninja01@gmail.com");
		credential.refreshIfExpired();
		return credential;
	}
	
	//Initialize google calendar service
	private Calendar getCalendarService( GoogleCredentials googleCredential) throws FileNotFoundException, IOException, GeneralSecurityException 
	{
		HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredential);
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
		
		Calendar calendar = new Calendar.Builder(httpTransport, JSON_FACTORY, requestInitializer)
				.setApplicationName(APPLICATION_NAME).build();
		return calendar;
	}

	//Get all events from the calendar
	public List<EventResponseDTO> getEventsUsingServiceAcc()
			throws Exception {
		try {
			com.google.api.services.calendar.model.Events eventList;
			
			Calendar calendar = getCalendarService(getServiceCredentials());

			String pageToken = null;
			List<EventResponseDTO> eventListResponse = new ArrayList<>();
			
			do {
			//eventList = events.list(CALENDAR_ID).setTimeMin(now).execute();
				eventList = calendar.events().list(CALENDAR_ID).setPageToken(pageToken).execute();
				pageToken = eventList.getNextPageToken();
				EventsMapper.mapToGCalendarEventResponseDTOList(eventList.getItems())
						.stream()
						.forEach(item-> eventListResponse.add(item));
			} while(pageToken!=null);
			
			return eventListResponse;
			
		} catch (Exception e) {
			logger.error("Exception:", e);
			throw new Exception(e.getLocalizedMessage());
		} 

	}

	
	// Creates an event in google calendar
	public void createEventUsingServiceAcc(EventDTO calendarEventRequestDTO)
			throws Exception {
		try {
			Calendar calendar = getCalendarService(getServiceCredentials());
			Events events = calendar.events();

			Event event = new Event().setAttachments(
					EventsMapper.mapToEventAttachment(calendarEventRequestDTO.getAttachments()))

			/*
			 * Google API throws 403-forbidden error, if attendees are set. TO set attendees,
			 * it needs domain wide delegation of authority, which is not currently enabled.
			 * Hence commenting this line. Once we have a domain account, we need to set
			 * domain wide delegation of authority and uncomment this line.
			 * https://support.google.com/a/answer/162106?hl=en#zippy=%2Cbefore-you-begin%
			 * 2Cset-up-domain-wide-delegation-for-a-client
			 */
//						.setAttendees(GCalendarEventsMapper.mapToEventAttendees(calendarEventRequestDTO.getAttendees()))
					.setDescription(calendarEventRequestDTO.getEventDescription())
					.setLocation(calendarEventRequestDTO.getLocation())
					.setSummary(calendarEventRequestDTO.getEventSummary());

			EventDateTime startDate = new EventDateTime();
			startDate.setDateTime(new DateTime(calendarEventRequestDTO.getEventStartDateTime()));
			// startDate.setTimeZone(TimeZone.getDefault().toString());
			event.setStart(startDate);

			EventDateTime endDate = new EventDateTime();
			endDate.setDateTime(new DateTime(calendarEventRequestDTO.getEventEndDateTime()));
			// endDate.setTimeZone(TimeZone.getDefault().toString());
			event.setEnd(endDate);

			Event eventInsertResponse = events.insert(CALENDAR_ID, event).execute();
			logger.debug(eventInsertResponse.toString());

		} catch (Exception e) {
			logger.error("Exception:", e);
			throw new Exception(e.getLocalizedMessage());
			
		} 
	}

	
	//Updates an existing event
	public void updateEvent( String eventId, @Valid EventDTO eventRequest) throws Exception {
		try {
			Calendar calendar = getCalendarService(getServiceCredentials());
			Events events = calendar.events();

			//Get  the event using the ID
			Event existingEvent = events.get(CALENDAR_ID, eventId).execute();
			
			//Update the event with the  user specified changes
			if(existingEvent != null) {

				/*
				 * Google API throws 403-forbidden error, if attendees are set. To set
				 * attendees, it needs domain wide delegation of authority, which is not
				 * currently enabled. Hence commenting this line. Once we have a domain account,
				 * we need to set domain wide delegation of authority and uncomment this line.
				 * https://support.google.com/a/answer/162106?hl=en#zippy=%2Cbefore-you-begin%
				 * 2Cset-up-domain-wide-delegation-for-a-client
				 */
	//						.setAttendees(GCalendarEventsMapper.mapToEventAttendees(calendarEventRequestDTO.getAttendees()))
				existingEvent.setDescription(eventRequest.getEventDescription());
				existingEvent.setLocation(eventRequest.getLocation());
				existingEvent.setSummary(eventRequest.getEventSummary());
	
				EventDateTime startDate = new EventDateTime();
				startDate.setDateTime(new DateTime(eventRequest.getEventStartDateTime()));
				existingEvent.setStart(startDate);
	
				EventDateTime endDate = new EventDateTime();
				endDate.setDateTime(new DateTime(eventRequest.getEventEndDateTime()));
				existingEvent.setEnd(endDate);
	
				events.update(CALENDAR_ID, eventId, existingEvent);
			}
		} catch (Exception e) {
			logger.error("Update event failed");
			logger.error("Exception:",e);
			
			throw new Exception(e.getLocalizedMessage());
		} 

	}

	//Deletes an existing event
	public void deleteEvent(String eventId) throws Exception {
		try {
			Calendar calendar = getCalendarService(getServiceCredentials());
			Events events = calendar.events();
			events.delete(CALENDAR_ID, eventId).execute();

		} catch (Exception e) {
			logger.error("Delete event failed for event id:" + eventId);
			logger.error("Exception: "+  e);
			throw new Exception(eventId);
		} 

	}
	
}
