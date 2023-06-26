// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.ObjectSource;

import java.util.function.Function;

final class MethodSupplierImpl<T> implements MethodSupplier<T> {
    private final Generator<T> generator;
    private final IntCache<T> dynamic;
    private final Function<Object, String[]> dynamicMethods;

    MethodSupplierImpl(Generator<T> generator, IntCache<T> dynamic, Function<Object, String[]> dynamicMethods) {
        this.generator = generator;
        this.dynamic = dynamic;
        this.dynamicMethods = dynamicMethods;
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<T> consumer) {
        var methods = generator.getMethods(object.getClass());
        for (var method : methods) consumer.accept(method.name(), method.method(), method);

        var dynamicMethods = this.dynamicMethods.apply(object);
        if (dynamicMethods != null) {
            for (var i = 0; i < dynamicMethods.length; i++) consumer.accept(dynamicMethods[i], dynamic.get(i), null);
        }

        return !methods.isEmpty() || dynamicMethods != null;
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<T> consumer) {
        var methods = generator.getMethods(object.getClass());
        for (var method : methods) consumer.accept(object, method.name(), method.method(), method);

        var hasMethods = !methods.isEmpty();

        if (object instanceof ObjectSource source) {
            for (var extra : source.getExtra()) {
                var extraMethods = generator.getMethods(extra.getClass());
                if (!extraMethods.isEmpty()) hasMethods = true;
                for (var method : extraMethods) consumer.accept(object, method.name(), method.method(), method);
            }
        }

        var dynamicMethods = this.dynamicMethods.apply(object);
        if (dynamicMethods != null) {
            hasMethods = true;
            for (var i = 0; i < dynamicMethods.length; i++) {
                consumer.accept(object, dynamicMethods[i], dynamic.get(i), null);
            }
        }

        return hasMethods;
    }
}
