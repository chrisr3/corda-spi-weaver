/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.corda.spi.weaving;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.BundleTrackerCustomizer;

class ConsumerBundleTrackerCustomizer implements BundleTrackerCustomizer<Object> {
    private static final Object DUMMY = new Object();

    private final DynamicExtraWeavingActivator activator;

    ConsumerBundleTrackerCustomizer(DynamicExtraWeavingActivator activator) {
        this.activator = activator;
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        try {
            activator.addConsumerWeavingData(bundle);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
        return DUMMY;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        removedBundle(bundle, event, object);
        addingBundle(bundle, event);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        activator.removeBundle(bundle);
    }
}
