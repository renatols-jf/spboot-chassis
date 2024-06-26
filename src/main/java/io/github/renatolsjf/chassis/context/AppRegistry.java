package io.github.renatolsjf.chassis.context;

import io.github.renatolsjf.chassis.Chassis;
import io.github.renatolsjf.chassis.request.Inject;
import io.github.renatolsjf.chassis.util.proxy.ChassisEnhancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppRegistry {

    private static Map<Class<?>, Object> instanceMap = new HashMap<>();

    private static ApplicationContext context;

    @Autowired
    AppRegistry(ApplicationContext ac) { context = ac; }

    public static ApplicationContext getContext() { return context; }

    public static <T> T getResource(Class<T> type) {

        //TODO I should probably store created types based on the delegate rather than the class. Inject
        //a service interface and inject a service implementation will yield different objects while spring will
        //provide the same object.
        Object instance = instanceMap.get(type);
        if (instance != null) {
            return (T) instance;
        }

        try {
            instance = context.getBean(type);
        } catch (Exception e) {
            //DO NOTHING
        }

        try {
            return (T) createObject(type, instance);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

    private static Object createObject(Class type, Object delegate) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {

        ChassisEnhancer chassisEnhancer = Chassis.getInstance().getEnhancer();
        Object object;
        if (chassisEnhancer.isEnhanceable(type)) {
            object = chassisEnhancer.enhance(type, delegate);
        }else if (delegate != null) {
            object = delegate;
        } else {
            Constructor<Object> c = type.getConstructor();
            object = c.newInstance();
        }

        Map<String, Object> currentInjections = new HashMap<>();
        Class<?> currentType;
        if (delegate != null) {
            currentType = delegate.getClass(); //should I call getSuperClass first?
            while (currentType != null && currentType != Object.class) {
                Field[] fields = currentType.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        field.trySetAccessible();
                        Object child = field.get(delegate);
                        child = createObject(field.getType(), child);
                        field.set(delegate, child);
                        currentInjections.put(field.getName(), child);
                    }
                }
                currentType = currentType.getSuperclass();
            }
        }

        //TODO it might not make sense to do this because an abstract class would be caught in the above loop and interfaces do not have fields.
        currentType = type;
        while (currentType != null && currentType != Object.class) {
            Field[] fields = currentType.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.trySetAccessible();
                    Object child = currentInjections.get(field.getName());
                    if (child == null) {
                        child = createObject(field.getType(), child);
                    }
                    field.set(object, child);
                }
            }
            currentType = currentType.getSuperclass();
        }

        if (delegate != null) {
            instanceMap.put(type, object);
        }

        return object;

    }

}
