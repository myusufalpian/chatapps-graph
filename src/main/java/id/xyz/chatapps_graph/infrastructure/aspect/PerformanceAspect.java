package id.xyz.chatapps_graph.infrastructure.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // Threshold in milliseconds. If a method takes longer than this, we log a warning.
    private static final long SLOW_THRESHOLD_MS = 1000;

    /**
     * Target all public methods in the UseCase/Service layer.
     */
    @Around("execution(* id.xyz.chatapps_graph.applications.usecase..*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Execute the method
        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // ONLY log if the method is too slow (Performance Bottleneck)
        if (duration > SLOW_THRESHOLD_MS) {
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();

            log.warn("⚠️ PERFORMANCE ALERT: {}.{} took {} ms (Threshold: {} ms)",
                    className, methodName, duration, SLOW_THRESHOLD_MS);
        }

        return result;
    }
}