package id.ac.ui.cs.advprog.donatjs.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ProfilerAspect {

    private static final Logger log = LoggerFactory.getLogger(ProfilerAspect.class);

    @Around("execution(* id.ac.ui.cs.advprog.donatjs.service.*.*(..))")
    public Object profileServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            log.info("PROFILING: Method {} executed in {} ms", joinPoint.getSignature().toShortString(), executionTime);
        }
    }
}
