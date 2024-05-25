package io.github.renatolsjf.chassis.util.genesis;

import io.github.renatolsjf.chassis.util.conversion.ConversionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExtractedMethod extends ExtractedMember<Method> {

    public ExtractedMethod(Object object, String memberName, Method member) {
        super(object, memberName, member);
    }

    @Override
    protected void doSet() throws UnableToSetMemberException {
        try {
            this.member.trySetAccessible();
            int numberOfParameters = this.member.getParameterTypes().length;
            int numberOfCalls = this.params.length / numberOfParameters;
            for (int i = 0; i < numberOfCalls; i++) {
                this.member.invoke(this.object, Arrays.copyOfRange(this.params,
                        numberOfParameters * i, numberOfParameters * i + numberOfParameters));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnableToSetMemberException(e);
        }
    }

    @Override
    protected void setParams(Object... params) {

        List<Class<?>> paramTypes = Arrays.stream(this.member.getParameterTypes())
                .map(c -> wrapperTypes.getOrDefault(c, c)).collect(Collectors.toList());

        if (params.length == 1 && params[0] instanceof Collection<?>) {
            this.setParams(((Collection<?>) params[0]).toArray());
        } else if (paramTypes.size() == params.length) {

            if (paramTypes.containsAll(Arrays.stream(params).map(Object::getClass).collect(Collectors.toList()))) {
                this.params = params;
                this.affinity = 0xF0 | affinity;
            } else if (IntStream.range(0, paramTypes.size())
                    .allMatch(i -> ConversionFactory.isConverterAvailable(params[i].getClass(), paramTypes.get(i)))) {
                this.params = IntStream.range(0, paramTypes.size())
                        .mapToObj(i -> ConversionFactory.converter(params[i].getClass(), paramTypes.get(i)).convert(params[i]))
                        .toArray();
                this.affinity = 0x90 | affinity;
            }

        } else if (params.length > paramTypes.size()
                && params.length % paramTypes.size() == 0
                && this.member.isAnnotationPresent(Multi.class)) {

            int numberOfParameters = paramTypes.size();
            if (paramTypes.containsAll(Arrays.stream(params).map(Object::getClass).collect(Collectors.toList()))) {
                this.params = params;
                this.affinity = 0x90 | affinity;
            } else if (IntStream.range(0, params.length)
                    .allMatch(i -> ConversionFactory.isConverterAvailable(params[i].getClass(), paramTypes.get(i % numberOfParameters)))) {
                this.params = IntStream.range(0, paramTypes.size())
                        .mapToObj(i -> ConversionFactory.converter(params[i].getClass(), paramTypes.get(i)).convert(params[i]))
                        .toArray();
                this.affinity = 0x70 | affinity;
            }

        }

    }

}