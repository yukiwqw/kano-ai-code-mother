package com.yupi.yuaicodemother.aop;

import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.Set;

/**
 * 统一校验控制层入参是否为空的切面。
 */
@Aspect
@Component
public class ControllerParamCheckAspect {

    /**
     * 需要忽略校验的类型集合。
     */
    private static final Set<Class<?>> IGNORE_TYPES = Set.of(
            HttpServletRequest.class,
            HttpServletResponse.class,
            HttpSession.class,
            Principal.class,
            MultipartFile.class
    );

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)"
            + " && within(com.yupi.yuaicodemother.controller..*)")
    public void controllerMethods() {
        // 切点：项目中控制层的公开接口方法
    }

    @Around("controllerMethods()")
    public Object validateRequestArgs(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            if (shouldSkip(parameter.getType())) {
                continue;
            }

            if (arg == null) {
                String paramName = parameter.getName();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数[" + paramName + "]不能为空");
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 判断是否跳过当前参数的校验。
     *
     * @param type 参数类型
     * @return 是否需要跳过
     */
    private boolean shouldSkip(Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        }
        if (type.isAnnotationPresent(RestController.class)) {
            return true;
        }
        return IGNORE_TYPES.stream().anyMatch(ignoreType -> ignoreType.isAssignableFrom(type));
    }
}

