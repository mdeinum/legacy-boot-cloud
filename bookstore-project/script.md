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

Put the following in `application.properties`

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
        <version>1.1.0.RELEASE</version>
    </dependency>

Modify the `BookstoreWebApplicationInitializer`.

1. Add interface `PriorityOrdered` to prevent the Jersey interfering with our `ContextLoaderListener`
2. Add an `ApplicationContextInitializer` to mimic the Spring Cloud Client bootstrapping

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

        servletContext.setInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM, BootstrapConfigInitializer.class.getName());
        servletContext.setInitParameter(ContextLoader.CONFIG_LOCATION_PARAM, CONFIG_LOCATION);
        servletContext.setInitParameter("spring.application.name", "bookstore");

5. Optionally remove the `location` attribute from the `<context:property-placeholder />`
6. Start the application


# Add Spring Boot as parent

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>1.3.5.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>

Now start the application and lets see what is going to happen.



## Let Spring Boot Manage the dependency versions (part 1)
1. Clean up the `<properties>` section, leave the Spring Framework version
2. Restart the application and check what happens

## Use spring-boot-starters to cleanup the pom
1. Add `spring-boot-starter-web`
2. Add `spring-boot-starter-data-jpa`
3. Add `spring-boot-starter-validation`
4. Add `spring-boot-starter-tomcat` with `<scope>provided</scope>`
5. Add

        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-jasper</artifactId>
            <scope>provided</scope>
        </dependency>

6. Cleanup the remainder of the dependencies section
7. Remove the `dependencyManagement` section of the pom (or at least clean it up)
8. Restart the application

## Upgrade Spring Version
1. Remove the `spring.version` from the `<properties>` section in the pom
2. Restart the application
3. Rollback the change
4. Create a test for the `OrderController`

        @RunWith(SpringJUnit4ClassRunner.class)
        @ContextConfiguration
        @WebAppConfiguration
        public class OrderControllerTest {

            private static final String EXPECTED_JSON = "{\"order\":{\"id\":1,\"shippingAddress\":null,\"billingAddress\":null,\"account\":{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"dateOfBirth\":null,\"address\":{\"street\":\"Nieuwstraat\",\"houseNumber\":\"1\",\"boxNumber\":\"A\",\"city\":\"Brussels\",\"postalCode\":\"1000\",\"country\":\"BE\"},\"emailAddress\":\"foo@test.com\",\"username\":\"jd\",\"password\":\"5238377ba2eac049901b54004ee9e03db62c0ab0b48133a4a162ab3aedfc809f\",\"roles\":[{\"id\":1,\"role\":\"ROLE_CUSTOMER\",\"permissions\":[{\"id\":1,\"permission\":\"PERM_CREATE_ORDER\"}]}]},\"billingSameAsShipping\":true,\"orderDate\":1463731858353,\"deliveryDate\":1463731858353,\"totalOrderPrice\":31.00,\"orderDetails\":[{\"id\":1,\"book\":{\"id\":1,\"title\":\"Effective Java\",\"description\":\"Brings together seventy-eight indispensable programmer's rules of thumb.\",\"price\":31.20,\"year\":2008,\"author\":\"Joshua Bloch\",\"category\":{\"id\":1,\"name\":\"IT\"},\"isbn\":\"9780321356680\"},\"quantity\":1,\"price\":31}],\"totalNumberOfbooks\":1}}";

            @Autowired
            private WebApplicationContext context;

            @Autowired
            private BookstoreService bookstoreService;

            @Autowired
            private AccountRepository accountRepository;

            private MockMvc mockMvc;

            @Before
            public void setup() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

            }

            @Test
            @Transactional
            @Ignore
            public void shouldReturnAJSonOrder() throws Exception {

                Account account = accountRepository.findByUsername("jd");

                mockMvc.perform(
                        get("/order/{orderId}", 1L)
                            .accept(MediaType.APPLICATION_JSON)
                            .sessionAttr(LoginController.ACCOUNT_ATTRIBUTE, account))
                        .andExpect(MockMvcResultMatchers.content().string(EXPECTED_JSON))
                        .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                        .andDo(MockMvcResultHandlers.print());

            }


            @Configuration
            @Import({WebMvcContextConfiguration.class, ViewConfiguration.class})
            @ImportResource("classpath:/META-INF/spring/application-context.xml")
            public static class OrderControllerTestConfiguration {
            }
        }
5. Run the test make sure it is green.
6. Modify in the `ViewConfiguration` the bean `MappingJacksonJsonView` to `MapingJackson2JsonView`
7. Remove Jackson1 from the pom and replace with Jackson2

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

8. Rerun the test make sure it is green again.
9. Start application check if it works
10. Goto 1. :)

## Use Spring Boot to bootstrap the application

1. Create a `BookstoreApplication` in the root package let it extend `SpringBootServletInitializer` and add a main method.
2. Annotate with `@SpringBootApplication` and remove our current bootstrap code. Add `@ImportResource` to load the current XML based configuration.
3. Register the `OpenEntityManagerInViewFilter` using an `@Bean` method and `FilterRegistrationBean`.
4. Remove the `BookstoreWebApplicationInitializer`.
5. Add the `spring-boot-maven-plugin` and remove the war and compile plugin (those are provided by the parent)
6. Restart the application using the main method.


## Add Spring Boot features
1. Add `spring-boot-starter-actuator`
2. Restart application
