package uk.ac.ebi.eva.submission.controller.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class SwaggerInterceptAdapter extends HandlerInterceptorAdapter {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Override
    /**
     * Redirecting to the swagger home page*/
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String req = request.getRequestURI();

        if (req.equals(contextPath) || req.equals(contextPath + "/") ||
                req.equals(contextPath + "/v1") || req.equals(contextPath + "/v1/")) {
            response.sendRedirect(contextPath + "/swagger-ui/index.html");
            return false;
        }

        return super.preHandle(request, response, handler);
    }
}
