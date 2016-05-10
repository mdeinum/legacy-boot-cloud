# Create Zuul Proxy
Using http://start.spring.io create bookstore-proxy Select web, config-client, zuul and actuator

Add `@EnableZuulProxy` to the created application class.

Use the following `application.yml`

    info:
      component: Bookstore Proxy

    server:
      port: 8080

    management:
      port: 8075

    endpoints:
      restart:
        enabled: true
      shutdown:
        enabled: true
      health:
        sensitive: false

    zuul:
      routes:
        legacy:
          path: /**
          url: http://localhost:8090/

Run the `BookstoreProxyApplication`.

Modify the `pom.xml`of the `bookstore` project, change the port from *8080* to *8090*.

## Reverse Proxy, header rewriting
Zuul isn't a full reverse proxy, we need to add a `ZuulFilter` to do `Location` header rewriting.

        package nl.conspect.bookstore.proxy;

        import java.util.Optional;

        import com.netflix.util.Pair;
        import com.netflix.zuul.ZuulFilter;
        import com.netflix.zuul.context.RequestContext;
        import org.springframework.cloud.netflix.zuul.filters.Route;
        import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
        import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
        import org.springframework.web.util.UrlPathHelper;

        /**
         * @author marten
         */
        public final class LocationHeaderRewritingFilter extends ZuulFilter {

            final UrlPathHelper urlPathHelper = new UrlPathHelper();
            final RouteLocator routeLocator;

            LocationHeaderRewritingFilter(RouteLocator routeLocator) {
                this.routeLocator = routeLocator;
            }

            @Override
            public String filterType() {
                return "post";
            }

            @Override
            public int filterOrder() {
                return 100;
            }

            @Override
            public boolean shouldFilter() {
                return locationHeader(RequestContext.getCurrentContext()) != null;
            }

            @Override
            public Object run() {
                RequestContext ctx = RequestContext.getCurrentContext();
                Route route = routeLocator.getMatchingRoute(urlPathHelper.getPathWithinApplication(ctx.getRequest()));
                if (route != null) {
                    Pair<String, String> lh = locationHeader(ctx);
                    if (lh != null) {
                        String location = lh.second().replace(
                                route.getLocation(),
                                ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + '/');
                        lh.setSecond(location);
                    }
                }
                return Boolean.TRUE;
            }

            private Pair<String, String> locationHeader(RequestContext ctx) {
                Optional<Pair<String, String>> header = ctx.getZuulResponseHeaders().stream().filter(h -> "Location".equals(h.first())).findFirst();
                return header.isPresent() ? header.get() : null;
            }
        }


# Add Configuration Server
Using http://start.spring.io create a config-service (config-server and actuator)

Add `@EnableConfigServer` to the application class.

Put the following in 'application.properties'

    server.port=8888
    spring.cloud.config.server.git.uri=file://${user.home}/Repositories/legacy-boot-cloud-config
    #spring.cloud.config.server.git.uri=https://github.com/mdeinum/legacy-boot-cloud-config.git

Reconfigure the bookstore-proxy to use the config server to obtain the configuration.
Add a `bootstrap.yml` file with the following content.

    spring:
      application:
        name: bookstore_proxy

Before restarting the proxy uncomment the config-client and remove or empty application.[yml|properties].

# Obtain Configuration from Config Server without Spring Boot

The "magic" is actually just configuration. Use the Spring Cloud Client configuration to manually obtain the
configuration.

First add dependency management for Spring Cloud by adding the bom

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.cloud</groupId>
				<artifactId>spring-cloud-dependencies</artifactId>
				<version>Brixton.RC2</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

Next add the `spring-cloud-config-client` dependency.

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-config-client</artifactId>
    </dependency>

Modify the `BookstoreWebApplicationInitializer`.

1. Add `@Order(Ordered.HIGHEST_PRECEDENCE)` to prevent the Jersey interfering with our `ContextLoaderListener`
2. Add an `ApplicationContextInitializer` to mimic theSpring Cloud Client bootstrapping

        public static class BootstrapConfigInitializer implements ApplicationContextInitializer {

            @Override
            public void initialize(ConfigurableApplicationContext applicationContext) {
                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                ConfigClientProperties configClientProperties = new ConfigClientProperties(environment);
                ConfigServicePropertySourceLocator locator = new ConfigServicePropertySourceLocator(configClientProperties);
                PropertySource config = locator.locate(environment);
                environment.getPropertySources().addLast(config);
            }
        }

3. Add init parameters for the configuration to load, the initializer to use and which configuration file to load).

        String[] initializers = new String[] {ConfigFileApplicationContextInitializer.class.getName(), BootstrapConfigInitializer.class.getName()};

        servletContext.setInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, StringUtils.arrayToCommaDelimitedString(initializers));
        servletContext.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, CONFIG_LOCATION);

4. Add `spring.application.name` to `bookstore.properties` and rename the file to `application.properties`
5. Optionally remove the `location` attribute from the `<context:property-placeholder />`
6. Start the application


# Add Spring Boot as parent

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.3.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>

Now start the application and lets see what is going to happen.

## Use spring-boot-starters to cleanup the pom
1. Add `spring-boot-starter-web`
2. Add `spring-boot-starter-tomcat` with `<scope>provided</scope>`
3. Add `spring-boot-starter-validation`
    We might need to set `<scope>provided</scope>` for the following dependency

    		<dependency>
    			<groupId>org.apache.tomcat.embed</groupId>
    			<artifactId>tomcat-embed-el</artifactId>
    		</dependency>

3. Add `spring-boot-starter-data-jpa`
4. Cleanup the remainder of the dependencies section
5. Remove the `dependencyManagement` section of the pom (or at least clean it up)
6. Restart the application

## Use Spring Boot to bootstrap the application

1. Create a `BookstoreApplication` in the root package let it extend `SpringBootServletInitializer` and add a main method.
2. Annotate with `@SpringBootApplication` and remove our current bootstrap code. Add `@ImportResource` to load the current XML based configuration.
3. Register the `OpenEntityManagerInViewFilter` using an `@Bean` method and `FilterRegistrationBean`.
4. Remove the `BookstoreWebApplicationInitializer`.
5. Add `server.port=8090` to `application.properties`
6. Add the `spring-boot-maven-plugin` and remove the war and compile plugin (those are provided by the parent)
7. Restart the application using the main method.

## Add Spring Boot features
1. Add `spring-boot-starter-actuator`
2. Restart application
