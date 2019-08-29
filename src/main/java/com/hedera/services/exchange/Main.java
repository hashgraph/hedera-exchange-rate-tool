package com.hedera.services.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class Main {

	public void execute(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final String configFileName = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		ExchangeRateTool.main(configFileName);
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@GetMapping("/")
	public String hello() throws Exception {
		ExchangeRateTool.main("https://storage.cloud.google.com/exchange-rate-config/config.json?authuser=3");
		return "Success!";
	}
}
