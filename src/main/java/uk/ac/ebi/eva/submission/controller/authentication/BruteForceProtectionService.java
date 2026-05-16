package uk.ac.ebi.eva.submission.controller.authentication;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BruteForceProtectionService implements ApplicationListener<AbstractAuthenticationFailureEvent> {

    private static final int MAX_ATTEMPTS = 10;
    private static final long LOCKOUT_DURATION_MS = 15 * 60_000L;

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockouts = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(AbstractAuthenticationFailureEvent event) {
        if (event.getAuthentication().getDetails() instanceof WebAuthenticationDetails) {
            String ip = ((WebAuthenticationDetails) event.getAuthentication().getDetails()).getRemoteAddress();
            int count = attempts.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
            if (count >= MAX_ATTEMPTS) {
                lockouts.put(ip, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            }
        }
    }

    public void onAuthenticationSuccess(String ip) {
        attempts.remove(ip);
        lockouts.remove(ip);
    }

    public boolean isBlocked(String ip) {
        Long until = lockouts.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            lockouts.remove(ip);
            attempts.remove(ip);
            return false;
        }
        return true;
    }
}
