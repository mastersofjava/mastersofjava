/*
   Copyright 2020 First Eight BV (The Netherlands)


   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file / these files except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.moj.server.bootstrap.filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.moj.common.bootstrap.BootstrapService;

@Component
@AllArgsConstructor
@Slf4j
public class BootstrapFilter extends OncePerRequestFilter {

    private final Pattern BOOTSTRAP = Pattern.compile("^.*/bootstrap(\\?.+)?$", Pattern.MULTILINE);
    private BootstrapService bootstrapService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            FilterChain filterChain) throws ServletException, IOException {
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
