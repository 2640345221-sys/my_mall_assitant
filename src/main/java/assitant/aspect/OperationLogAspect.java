package assitant.aspect;

import assitant.annotation.OperationLog;
import assitant.entity.annotation.OperationLogPO;
import assitant.utils.JSONDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Pointcut("@annotation(assitant.annotation.OperationLog)")
    public void operationLogPointcut() {}

    @Around("operationLogPointcut()")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog op = method.getAnnotation(OperationLog.class);

        OperationLogPO po = OperationLogPO.builder()
                .module(op.module()).type(op.type()).description(op.description())
                .recordParams(op.recordParams()).recordResult(op.recordResult())
                .createTime(LocalDateTime.now()).build();

        if (op.recordParams()) po.setParams(JSONDataUtils.formatParams(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            po.setExecutionTime(System.currentTimeMillis() - start);
            if (op.recordResult()) po.setResult(JSONDataUtils.formatResult(result));
            log.info("操作日志 [{}] {} {} | 参数:{} | 耗时:{}ms | 结果:{}",
                    po.getModule(), po.getType(), po.getDescription(),
                    po.getParams(), po.getExecutionTime(), po.getResult());
            return result;
        } catch (Throwable e) {
            po.setExecutionTime(System.currentTimeMillis() - start);
            po.setErrorMessage(e.getMessage());
            log.error("操作失败 [{}] {} {} | 参数:{} | 耗时:{}ms | 错误:{}",
                    po.getModule(), po.getType(), po.getDescription(),
                    po.getParams(), po.getExecutionTime(), e.getMessage());
            throw e;
        }
    }
}
