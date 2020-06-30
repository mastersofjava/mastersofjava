package nl.moj.server.util;

import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class HttpUtil {
    public static String getParam(String param) {
        return getParam(param, null);
    }
    public static String getParam(String param, String defaultVal) {
        HttpServletRequest req = getCurrentHttpRequest();
        return req==null?defaultVal: req.getParameter(param);
    }
    public static boolean hasParam(String param) {
        HttpServletRequest req = getCurrentHttpRequest();
        return req==null?false: req.getParameterMap().containsKey(param);
    }
    public static String getCurrentHttpRequestUserName() {
        SecurityContextImpl context = (SecurityContextImpl)getCurrentHttpRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        return context.getAuthentication().getName();
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
