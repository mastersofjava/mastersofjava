package nl.moj.server.bootstrap.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.server.bootstrap.service.BootstrapService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter(urlPatterns = {"*"})
@AllArgsConstructor
@Slf4j
public class BootstrapFilter extends OncePerRequestFilter {

    private final Pattern BOOTSTRAP = Pattern.compile("^.*/bootstrap(\\?.+)?$", Pattern.MULTILINE);
    private BootstrapService bootstrapService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (!isBootstrapUrl(httpServletRequest) && bootstrapService.isBootstrapNeeded()) {
            httpServletResponse.sendRedirect(getServletContext().getContextPath() + "/bootstrap");
        } else {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }

    private boolean isBootstrapUrl(HttpServletRequest request) {
        Matcher requestMatcher = BOOTSTRAP.matcher(request.getRequestURI());
        Matcher refererMatcher = null;

        if (request.getHeader(HttpHeaders.REFERER) != null) {
            refererMatcher = BOOTSTRAP.matcher(request.getHeader(HttpHeaders.REFERER));
        }

        // ignore bootstrap and all resources for that page based on referer url.
        return requestMatcher.matches() || (refererMatcher != null && refererMatcher.matches());

    }
}
