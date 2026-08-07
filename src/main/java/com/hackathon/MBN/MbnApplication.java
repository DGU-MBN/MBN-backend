package com.hackathon.MBN;

import com.hackathon.MBN.news.WatchlistProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WatchlistProperties.class)
public class MbnApplication {

	public static void main(String[] args) {
		SpringApplication.run(MbnApplication.class, args);
	}

}
