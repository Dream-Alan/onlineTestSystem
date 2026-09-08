package dream.maven.demoProject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dream.maven.demoProject.common.Result;
import dream.maven.demoProject.common.UserContext;
import dream.maven.demoProject.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtFilter extends HttpFilter {

    private static final String[] WHITE_LIST = {"/auth/login"};

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isWhiteListed(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorized(response, "未授权，请先登录");
            return;
        }

        String token = authHeader.substring(7);
        try {
            var claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);
            UserContext.set(new UserContext(userId, role));
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "登录已过期，请重新登录");
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhiteListed(String uri) {
        for (String path : WHITE_LIST) {
            if (uri.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, message)));
    }
}
