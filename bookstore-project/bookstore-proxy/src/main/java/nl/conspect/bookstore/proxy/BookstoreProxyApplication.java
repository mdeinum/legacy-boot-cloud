package nl.conspect.bookstore.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableZuulProxy
public class BookstoreProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookstoreProxyApplication.class, args);
	}

	@Bean
	public LocationHeaderRewritingFilter headerUrlRewritingFilter(RouteLocator locator) {
		return new LocationHeaderRewritingFilter(locator);
	}
}
