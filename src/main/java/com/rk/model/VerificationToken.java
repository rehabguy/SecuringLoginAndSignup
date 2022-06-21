package com.rk.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Document(collection = "VerificationToken")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerificationToken {
	@Id
	private String id;
	private String token;
	private User user;
	private Instant expiryDate;
}
