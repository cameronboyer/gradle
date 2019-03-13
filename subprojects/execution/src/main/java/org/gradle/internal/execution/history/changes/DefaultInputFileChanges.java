/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.history.changes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.gradle.internal.change.ChangeContainer;
import org.gradle.internal.change.ChangeVisitor;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;

import java.util.SortedMap;

public class DefaultInputFileChanges extends AbstractFingerprintChanges implements InputFileChanges {
    private static final String TITLE = "Input";

    public DefaultInputFileChanges(SortedMap<String, FileCollectionFingerprint> previous, SortedMap<String, CurrentFileCollectionFingerprint> current) {
        super(previous, current, TITLE);
    }

    @Override
    public boolean accept(ChangeVisitor visitor) {
        return accept(visitor, true);
    }

    @Override
    public boolean accept(String propertyName, ChangeVisitor visitor) {
        CurrentFileCollectionFingerprint currentFileCollectionFingerprint = current.get(propertyName);
        FileCollectionFingerprint previousFileCollectionFingerprint = previous.get(propertyName);
        return currentFileCollectionFingerprint.visitChangesSince(previousFileCollectionFingerprint, TITLE, true, visitor);
    }

    @Override
    public ChangeContainer nonIncrementalChanges(Iterable<String> incrementalPropertyNames) {
        ImmutableSet<String> unfilteredPropertyNames = ImmutableSet.copyOf(incrementalPropertyNames);
        if (unfilteredPropertyNames.isEmpty()) {
            // No incremental inputs declared.
            // For backwards compatibility, we allow changes to all inputs.
            return ChangeContainer.EMPTY;
        }

        return new DefaultInputFileChanges(
            Maps.filterKeys(previous, propertyName -> !unfilteredPropertyNames.contains(propertyName)),
            Maps.filterKeys(current, propertyName -> !unfilteredPropertyNames.contains(propertyName))
        );
    }
}
