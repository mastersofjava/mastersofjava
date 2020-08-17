package nl.moj.server.restcontrollers;

import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HomeController  {


    @GetMapping("/home")
    public void process(HttpServletRequest request, HttpServletResponse response,
                            ServletContext servletContext, TemplateEngine templateEngine) {

        WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());

        try {
            templateEngine.process("index", ctx, response.getWriter());

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

}
