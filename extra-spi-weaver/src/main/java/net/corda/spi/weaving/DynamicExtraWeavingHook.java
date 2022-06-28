package net.corda.spi.weaving;

import org.apache.aries.spifly.WeavingData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

import java.util.Set;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

final class DynamicExtraWeavingHook implements WeavingHook {
    private final DynamicExtraWeavingActivator activator;

    DynamicExtraWeavingHook(DynamicExtraWeavingActivator activator) {
        this.activator = activator;
    }

    @Override
    public void weave(WovenClass wovenClass) {
        Bundle consumerBundle = wovenClass.getBundleWiring().getBundle();
        Set<WeavingData> weavingData = activator.getWeavingData(consumerBundle);
        if (weavingData!= null) {
            ClassReader cr = new ClassReader(wovenClass.getBytes());
            ClassWriter cw = new OSGiFriendlyClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES, wovenClass.getBundleWiring());
            TCCLSetterVisitor tsv = new TCCLSetterVisitor(cw, wovenClass.getClassName(), weavingData);
            cr.accept(tsv, SKIP_FRAMES);
            if (tsv.isWoven()) {
                wovenClass.setBytes(cw.toByteArray());
                wovenClass.getDynamicImports().addAll(tsv.getExtraImports());
            }
        }
    }
}
