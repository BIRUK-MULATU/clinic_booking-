package et.aau.clinic.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * The whole of our "session-based login": every page except /login
 * requires a patientId in the HttpSession, put there by LoginController
 * on successful authentication. No Spring Security, per CLAUDE.md -
 * this is the simplest thing that gives every other page a logged-in
 * patient to work with.
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("patientId") == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}
