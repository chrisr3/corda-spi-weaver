package net.corda.spi.weaving;

import aQute.bnd.header.Parameters;
import aQute.bnd.stream.MapStream;
import aQute.libg.glob.Glob;
import org.apache.aries.spifly.BundleDescriptor;
import org.apache.aries.spifly.ConsumerRestriction;
import org.apache.aries.spifly.WeavingData;
import org.osgi.annotation.bundle.Header;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.util.tracker.BundleTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.INSTALLED;
import static org.osgi.framework.Bundle.RESOLVED;
import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Constants.EXTENSION_BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.EXTENSION_DIRECTIVE;
import static org.osgi.framework.Constants.EXTENSION_FRAMEWORK;
import static org.osgi.framework.Constants.FILTER_DIRECTIVE;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.REQUIRE_CAPABILITY;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_SYMBOLICNAME;

@SuppressWarnings("unused")
@Requirement(namespace = "osgi.extender", filter = "(osgi.extender=osgi.serviceloader.registrar)")
@Header(name = FRAGMENT_HOST, value = SYSTEM_BUNDLE_SYMBOLICNAME + ';' + EXTENSION_DIRECTIVE + ":=" + EXTENSION_FRAMEWORK)
@Header(name = EXTENSION_BUNDLE_ACTIVATOR, value = "${@class}")
public final class DynamicExtraWeavingActivator implements BundleActivator {
    static final Logger logger = Logger.getAnonymousLogger();

    private static final String CLIENT_REQUIREMENT = "osgi.extender;"
        + FILTER_DIRECTIVE + ":='(osgi.extender=osgi.serviceloader.processor)'";

    private static final Set<WeavingData> NON_WOVEN_BUNDLE = emptySet();

    private final ConcurrentMap<Bundle, Map<ConsumerRestriction, List<BundleDescriptor>>> consumerRestrictions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Bundle, Set<WeavingData>> bundleWeavingData = new ConcurrentHashMap<>();

    private ServiceRegistration<WeavingHook> weaver;
    private BundleTracker<?> consumerBundleTracker;
    private Parameters autoConsumerInstructions;

    @Override
    public void start(@Nonnull BundleContext context) throws Exception {
        String autoConsumers = context.getProperty("net.corda.spi-weaver.auto.consumers");
        autoConsumerInstructions = (autoConsumers != null) ? new Parameters(autoConsumers) : null;

        WeavingHook weavingHook = new DynamicExtraWeavingHook(this);
        weaver = context.registerService(WeavingHook.class, weavingHook, null);

        consumerBundleTracker = new BundleTracker<>(context, INSTALLED | RESOLVED | STARTING | ACTIVE, new ConsumerBundleTrackerCustomizer(this));
        consumerBundleTracker.open();

        for (Bundle bundle : context.getBundles()) {
            addConsumerWeavingData(bundle);
        }
    }

    @Override
    public void stop(BundleContext context) {
        consumerBundleTracker.close();
        weaver.unregister();
    }

    void addConsumerWeavingData(Bundle bundle) throws InvalidSyntaxException {
        if (bundleWeavingData.containsKey(bundle)) {
            // This bundle was already processed
            return;
        }

        Map<String, List<String>> allHeaders = new HashMap<>();
        MapStream.ofNullable(autoConsumerInstructions).filterKey(key ->
            Glob.toPattern(key).asPredicate().test(bundle.getSymbolicName())
        ).findFirst().ifPresent(un ->
            allHeaders.put(REQUIRE_CAPABILITY, singletonList(CLIENT_REQUIREMENT + ",osgi.serviceloader;filter:='(osgi.serviceloader=*)'"))
        );

        Set<WeavingData> weavingData = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : allHeaders.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                weavingData.addAll(ConsumerHeaderProcessor.processRequireCapabilityHeader(headerValue));
            }
        }

        if (weavingData.isEmpty()) {
            bundleWeavingData.put(bundle, NON_WOVEN_BUNDLE);
        } else {
            bundleWeavingData.put(bundle, unmodifiableSet(weavingData));

            for (WeavingData data : weavingData) {
                registerConsumerBundle(bundle, data.getArgRestrictions(), data.getAllowedBundles());
            }
        }
    }

    void registerConsumerBundle(Bundle consumerBundle,
                                @Nonnull Set<ConsumerRestriction> restrictions,
                                List<BundleDescriptor> allowedBundles) {
        Map<ConsumerRestriction, List<BundleDescriptor>> map = consumerRestrictions.computeIfAbsent(
            consumerBundle, k -> new HashMap<>()
        );
        for (ConsumerRestriction restriction : restrictions) {
            map.put(restriction, allowedBundles);
        }
    }

    void removeWeavingData(Bundle bundle) {
        bundleWeavingData.remove(bundle);
    }

    @Nullable
    Set<WeavingData> getWeavingData(Bundle bundle) {
        // Simply return the value as it's already an immutable set.
        Set<WeavingData> weavingData = bundleWeavingData.get(bundle);
        return weavingData == null || weavingData.isEmpty() ? null : weavingData;
    }
}
