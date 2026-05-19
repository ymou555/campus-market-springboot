package org.example.campusmarket.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    @Pointcut("execution(* org.example.campusmarket.service.*.*(..))")
    public void serviceMethods() {}
    
    @Around("serviceMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        logger.info("【方法调用】{}.{}() - 参数: {}", className, methodName, args);
        
        try {
            Object result = joinPoint.proceed();
            
            logger.info("【方法返回】{}.{}() - 结果: {}", className, methodName, result);
            
            return result;
        } catch (Exception e) {
            logger.error("【方法异常】{}.{}() - 异常: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
