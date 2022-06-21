package com.rk.service;

import com.rk.dto.AuthenticationResponse;
import com.rk.dto.LoginRequest;
import com.rk.dto.RefreshTokenRequest;
import com.rk.dto.RegisterRequest;
import com.rk.exceptions.SpringRedditException;
import com.rk.model.NotificationEmail;
import com.rk.model.User;
import com.rk.model.VerificationToken;
import com.rk.repository.UserRepository;
import com.rk.repository.VerificationTokenRepository;
import com.rk.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private VerificationTokenRepository verificationTokenRepository;
	
	@Autowired
	private MailService mailService; 
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Transactional
	public void signup(RegisterRequest registerRequest) {
		User user=new User();
		user.setUsername(registerRequest.getUsername());
		user.setEmail(registerRequest.getEmail());
		user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
	//	user.setCreated(Instant.now());
		user.setEnabled(false);
		userRepository.save(user);
		
		String token=generateVerificationToken(user);
		mailService.Sendmail(new NotificationEmail("Please activate your account    ", user.getEmail(),"Thank you for signing up"+"please click on the url to activate the account"+
		"http://localhost:8080/api/auth/accountVerification/"+token));
	}
	
	private String generateVerificationToken(User user){
		String token=UUID.randomUUID().toString();
		VerificationToken verificationToken=new VerificationToken();
		verificationToken.setToken(token);
		verificationToken.setUser(user);
		verificationTokenRepository.save(verificationToken);
		return token;
	}

	public void verifyAccount(String token) {
		Optional<VerificationToken> verificationToken= verificationTokenRepository.findByToken(token);
		verificationToken.orElseThrow(()->new SpringRedditException("Invalid token"));
		fetchUserAndEnable(verificationToken.get());
	}

	@Transactional
	private void fetchUserAndEnable(VerificationToken verificationToken) {
		String username=verificationToken.getUser().getUsername();
		User user=userRepository.findByUsername(username).orElseThrow(()->new SpringRedditException("No username found"));
		user.setEnabled(true);
		userRepository.save(user);
	}

	public AuthenticationResponse login(LoginRequest loginRequest) {
		Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken
				(loginRequest.getUsername(),
				loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authenticate);
		String token = jwtProvider.generateToken(authenticate);
		return AuthenticationResponse.builder()
				.authenticationToken(token)
				.refreshToken(refreshTokenService.generateRefreshToken().getToken())
				.expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
				.username(loginRequest.getUsername())
				.build();
	}

	public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest){
		refreshTokenService.validateRefreshToken(refreshTokenRequest.getRefreshToken());
		String token=jwtProvider.generateTokenWithUserName(refreshTokenRequest.getUsername());
		return AuthenticationResponse.builder()
				.authenticationToken(token)
				.refreshToken(refreshTokenRequest.getRefreshToken())
				.expiresAt(Instant.now().plusMillis(jwtProvider.getJwtExpirationInMillis()))
				.username(refreshTokenRequest.getUsername())
				.build();
	}

	@Transactional
	public User getCurrentUser(){
		org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.
				getContext().getAuthentication().getPrincipal();
		return userRepository.findByUsername(principal.getUsername())
				.orElseThrow(() -> new UsernameNotFoundException("User name not found - " + principal.getUsername()));

	}
}
