// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.metrics;

import com.google.common.base.CaseFormat;
import dan200.computercraft.core.metrics.Metric;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.metrics.basic.Aggregate;
import dan200.computercraft.shared.computer.metrics.basic.AggregatedMetric;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * An MBean which exposes aggregate statistics about all computers on the server.
 */
public final class ComputerMBean implements DynamicMBean, ComputerMetricsObserver {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerMBean.class);

    private static @Nullable ComputerMBean instance;

    private final Map<String, LongSupplier> attributes = new HashMap<>();
    private final Int2ObjectMap<Counter> values = new Int2ObjectOpenHashMap<>();
    private final MBeanInfo info;

    private ComputerMBean() {
        Metrics.init();

        List<MBeanAttributeInfo> attributes = new ArrayList<>();
        for (var field : Metric.metrics().entrySet()) {
            var name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, field.getKey());
            add(name, field.getValue(), attributes);
        }

        info = new MBeanInfo(
            ComputerMBean.class.getSimpleName(),
            "metrics about all computers on the server",
            attributes.toArray(new MBeanAttributeInfo[0]), null, null, null
        );
    }

    public static void register() {
        if (instance != null) return;

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(instance = new ComputerMBean(), new ObjectName("dan200.computercraft:type=Computers"));
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException |
                 MalformedObjectNameException e) {
            LOG.warn("Failed to register JMX bean", e);
        }
    }

    public static void start(MinecraftServer server) {
        if (instance != null) ServerContext.get(server).metrics().addObserver(instance);
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException {
        var value = attributes.get(attribute);
        if (value == null) throw new AttributeNotFoundException();
        return value.getAsLong();
    }

    @Override
    public void setAttribute(Attribute attribute) throws InvalidAttributeValueException {
        throw new InvalidAttributeValueException("Cannot set attribute");
    }

    @Override
    public AttributeList getAttributes(String[] names) {
        var result = new AttributeList(names.length);
        for (var name : names) {
            var value = attributes.get(name);
            if (value != null) result.add(new Attribute(name, value.getAsLong()));
        }
        return result;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList();
    }

    @Nullable
    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    private void observe(Metric field, long change) {
        var counter = values.get(field.id());
        counter.value.addAndGet(change);
        counter.count.incrementAndGet();
    }

    @Override
    public void observe(ServerComputer computer, Metric.Counter counter) {
        observe(counter, 1);
    }

    @Override
    public void observe(ServerComputer computer, Metric.Event event, long value) {
        observe(event, value);
    }

    private MBeanAttributeInfo addAttribute(String name, String description, LongSupplier value) {
        attributes.put(name, value);
        return new MBeanAttributeInfo(name, "long", description, true, false, false);
    }

    private void add(String name, Metric field, List<MBeanAttributeInfo> attributes) {
        var counter = new Counter();
        values.put(field.id(), counter);

        var prettyName = new AggregatedMetric(field, Aggregate.NONE).displayName().getString();
        attributes.add(addAttribute(name, prettyName, counter.value::longValue));
        if (field instanceof Metric.Event) {
            var countName = new AggregatedMetric(field, Aggregate.COUNT).displayName().getString();
            attributes.add(addAttribute(name + "Count", countName, counter.count::longValue));
        }
    }

    private static final class Counter {
        final AtomicLong value = new AtomicLong();
        final AtomicLong count = new AtomicLong();
    }
}
