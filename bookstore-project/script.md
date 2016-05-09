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
    spring.cloud.config.server.git.uri=https://github.com/mdeinum/legacy-boot-cloud-config.git

Reconfigure the bookstore-proxy to use the config server to obtain the configuration.
Add a `bootstrap.yml` file with the following content.

    spring:
      application:
        name: bookstore_proxy

Before restarting the proxy uncomment the config-client and remove or empty application.[yml|properties].

# Obtain Configuration from Config Server without Config Client

Using a `RestTemplate` call the config-service and obtain the configuration for the bookstore application.
(http://localhost:8888/bookstore.properties).

    public class ConfigServerInitializer implements ApplicationContextInitializer {

        public void initialize(ConfigurableApplicationContext ctx) {
            RestTemplate rest = new RestTemplate();
            Map properties = rest.getForObject("http://localhost:8888/bookstore.properties", Map.class);
            ctx.getEnvironment().addPropertySource("config-server", new MapPropertySource(properties));
        }

    }

# Add Spring Boot as parent

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.3.4.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>