package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

	//import Calendar.CalendarSample;


	@RestController
	public class CalendarController {
		//Application name
		@Value("${spring.application.name}")
		private String APPLICATION_NAME ;
//		
//		@Value("${google.client-id}")
//		private  String CLIENT_ID;
//		
//		@Value("${google.client-secret}")
//		private  String CLIENT_SECRET;
//		
//		@Value("${google.redirect-uri}")
//		private  String REDIRECT_URI;
		
		//This JSON model to hold client id and secret
		private GoogleClientSecrets clientSecrets;
		
		//This is used to get oAuth2 token from Google
		private GoogleAuthorizationCodeFlow flow;
		
		//OAuth 2.0 helper for accessing protected resources using an access token, 
		//as well as optionally refreshing the access token when it expires using a refresh token.
		private Credential credential;

		private static HttpTransport httpTransport;
		private static Calendar client;
		
		 //Global instance of the JSON factory.
		  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
		  
		  //Data store file
		  private static final String TOKENS_DIR_PATH = "tokens";

		//This is to tell what components we will be accessing, currently i have set it to READ_ONLY,
		//This might be changed if we create, delete events from our code
		private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
				
		private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);
		
		final DateTime date1 = new DateTime("2017-05-05T16:30:00.000+05:30");
		final DateTime toDate2 = new DateTime(new Date());
		
		@RequestMapping(value = "/login/gcalendar", method = RequestMethod.GET)
		public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception {
			return new RedirectView(authorize());
		}

		private String authorize() throws GeneralSecurityException, IOException  {
			AuthorizationCodeRequestUrl authCodeRequestUrl;
			if(flow == null)
			{
				Details credentialDetails = new Details();
				credentialDetails.setClientId(CLIENT_ID);
				credentialDetails.setClientSecret(CLIENT_SECRET);
			
				clientSecrets = new GoogleClientSecrets().setWeb(credentialDetails);
				
				 httpTransport = GoogleNetHttpTransport.newTrustedTransport();
				
				flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR_PATH)))
						.build();
				
			}
			authCodeRequestUrl = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI);
			logger.info("authCodeRequestUrl -> " + authCodeRequestUrl);
				
			return authCodeRequestUrl.build();
		}
		
		@RequestMapping(value = "/login/oauth2/code/google", method = RequestMethod.GET, params = "code")
		public ResponseEntity<String> oauth2Callback(@RequestParam(value = "code") String code ) throws IOException {
			
			com.google.api.services.calendar.model.Events eventList;
			String message;
			try {
				
				credential = getCredentials(code);
				//credential.refreshToken();
				client = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
						.setApplicationName(APPLICATION_NAME)
						.build();
				
				eventList = client.events().list("primary").execute();
				message = eventList.getItems().toString();
			} catch (Exception e) {
				logger.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")."
						+ " Redirecting to google connection status page.");
				message = "Exception while handling OAuth2 callback (" + e.getMessage() + ")."
						+ " Redirecting to google connection status page.";
			}

			return new ResponseEntity<>(message, HttpStatus.OK);
			
		}
		
		private Credential getCredentials(String code) throws IOException {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
			return flow.createAndStoreCredential(response, "userID");

		}
//		private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
//				   throws IOException {
//				 // Load client secrets.
//				 InputStream in = CalendarSample.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
//				// if (in == null) {
//				//   throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
//				// }
//				 
//				
//				 GoogleClientSecrets clientSecrets =
//				     GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
	//
//				 // Build flow and trigger user authorization request.
//				 GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//				     HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//				     .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//				     .setAccessType("offline")
//				     .build();
//				 LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
//				 Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//				 //returns an authorized Credential object.
//				 return credential;
//				}


//		@RequestMapping(value = "/events")
//		public ResponseEntity<String> getEvents(@AuthenticationPrincipal OAuth2User oAuth2User, 
//		        @RequestParam(value = "sdate") String sdate, 
//		        @RequestParam(value = "edate") String edate, 
//		        @RequestParam(value = "q") String q) {
//		    com.google.api.services.calendar.model.Events eventList;
//		    String message;
//		    try {
//		       // CustomOAuth2User customOAuth2User = (CustomOAuth2User)oAuth2User;
//		    	FileInputStream fis = new FileInputStream(new File("StoredCredential"));
//		        String token = fis.readAllBytes().toString();//customOAuth2User.getToken();
//		        GoogleCredential credential = new GoogleCredential().setAccessToken(token);
//		        
//		        final DateTime date1 = new DateTime(sdate + "T00:00:00");
//		        final DateTime date2 = new DateTime(edate + "T23:59:59");
//		        
//		        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
//		        Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
//		                .setApplicationName(APPLICATION_NAME).build();
//		        Events events = service.events();
//		        eventList = events.list("primary").setTimeZone("Asia/Kolkata").setTimeMin(date1).setTimeMax(date2).setQ(q).execute();
//		        message = eventList.getItems().toString();
//		        System.out.println("My:" + eventList.getItems());
//		    } catch (Exception e) {
//		        
//		        message = "Exception while handling OAuth2 callback (" + e.getMessage() + ")."
//		                + " Redirecting to google connection status page.";
//		    }
	//
//		    return new ResponseEntity<>(message, HttpStatus.OK);
//		}
	}



