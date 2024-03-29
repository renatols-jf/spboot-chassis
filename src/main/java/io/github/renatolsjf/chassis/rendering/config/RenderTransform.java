package io.github.renatolsjf.chassis.rendering.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RenderTransform {
    Class<? extends RenderTransformer> value() default DefaultRenderTransformer.class;
}
