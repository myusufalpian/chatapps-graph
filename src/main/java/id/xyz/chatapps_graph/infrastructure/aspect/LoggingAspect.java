package id.xyz.chatapps_graph.infrastructure.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    /**
     * Pointcut that targets all classes inside the 'applications.usecase' package.
     * "execution(* id.xyz..applications.usecase..*(..))"
     * breakdown:
     * - * : Any return type
     * - id.xyz..applications.usecase.. : Package path
     * - * : Any class
     * - (..) : Any arguments
     */
    @Pointcut("execution(* id.xyz.chatapps_graph.applications.usecase..*(..))")
    public void useCaseLayer() {}

    /**
     * @Around allows you to run code BEFORE and AFTER the method.
     */
    @Around("useCaseLayer()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 1. Log Method Entry
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("▶ START: {}.{}() with args: {}", className, methodName, Arrays.toString(args));

        Object result;
        try {
            // 2. Execute the actual method
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // Optional: Log exception before throwing it up
            log.error("⚠ EXCEPTION in {}.{}: {}", className, methodName, e.getMessage());
            throw e;
        }

        // 3. Log Method Exit & Duration
        long duration = System.currentTimeMillis() - start;
        log.info("◀ END: {}.{}() - Taken: {} ms", className, methodName, duration);

        return result;
    }
}