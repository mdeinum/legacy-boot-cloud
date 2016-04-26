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
