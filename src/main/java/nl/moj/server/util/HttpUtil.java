package nl.moj.server.util;

import nl.moj.server.teams.model.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;

public class HttpUtil {
    public static String getParam(String param) {
        return getParam(param, null);
    }
    public static String getParam(String param, String defaultVal) {
        HttpServletRequest req = getCurrentHttpRequest();
        return req==null||req.getParameter(param)==null?defaultVal: req.getParameter(param);
    }
    public static boolean hasParam(String param) {
        HttpServletRequest req = getCurrentHttpRequest();
        return req==null?false: req.getParameterMap().containsKey(param);
    }
    public static String getCurrentHttpRequestUserName() {
        SecurityContextImpl context = (SecurityContextImpl)getCurrentHttpRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        return context.getAuthentication().getName();
    }
    public static boolean isWithAdminRole(Principal user) {
        if (!(user instanceof UsernamePasswordAuthenticationToken)) {
            return false;
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)user;
        boolean isEmpty = authenticationToken.getAuthorities().isEmpty();
        if (isEmpty) {
            return false;
        }
        GrantedAuthority authority = authenticationToken.getAuthorities().iterator().next();

        boolean isWithAdminRole = Role.ADMIN.equals(authority.getAuthority())
                ||Role.GAME_MASTER.equals(authority.getAuthority());
        return isWithAdminRole;
    }
    public static boolean isAuthorizedForAssignmentEditing(Principal user, HttpServletRequest request) {
        return isWithAdminRole(user) && request.getParameterMap().containsKey("assignment");
    }
    public static UUID getSelectedUserSession(UUID defaultVal) {
        UUID val = (UUID)getCurrentHttpRequest().getSession().getAttribute("selectedUserSession");
        if (val==null) {
            val = defaultVal;
        }
        return val;
    }
    public static Object getSessionAttribute(String attributeName, Object defaultVal) {
        Object val = (Object)getCurrentHttpRequest().getSession().getAttribute(attributeName);
        return val==null?defaultVal:val;
    }
    public static void setSelectedUserSession(UUID sessionId) {
        getCurrentHttpRequest().getSession().setAttribute("selectedUserSession",sessionId);
    }
    public static HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

}
