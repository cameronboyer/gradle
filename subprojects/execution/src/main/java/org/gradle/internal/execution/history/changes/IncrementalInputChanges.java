/*
 * Copyright 2019 the original author or authors.
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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.internal.Cast;
import org.gradle.internal.change.CollectingChangeVisitor;
import org.gradle.work.FileChange;

public class IncrementalInputChanges implements InputChangesInternal {

    private final InputFileChanges changes;
    private final ImmutableMultimap<Object, String> propertyNamesByValue;

    public IncrementalInputChanges(InputFileChanges changes, ImmutableMultimap<Object, String> propertyNamesByValue) {
        this.changes = changes;
        this.propertyNamesByValue = propertyNamesByValue;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public Iterable<FileChange> getFileChanges(Object parameterValue) {
        String propertyName = determinePropertyName(parameterValue, propertyNamesByValue);
        CollectingChangeVisitor visitor = new CollectingChangeVisitor();
        changes.accept(propertyName, visitor);
        return Cast.uncheckedNonnullCast(visitor.getChanges());
    }

    public static String determinePropertyName(Object propertyValue, ImmutableMultimap<Object, String> propertyNameByValue) {
        ImmutableCollection<String> propertyNames = propertyNameByValue.get(propertyValue);
        if (propertyNames.isEmpty()) {
            throw new UnsupportedOperationException("Cannot query incremental changes: No property found for value " + propertyValue + ".");
        }
        if (propertyNames.size() > 1) {
            throw new UnsupportedOperationException(String.format("Cannot query incremental changes: More that one property found with value %s: %s.", propertyValue, propertyNames));
        }
        return propertyNames.iterator().next();
    }

    @Override
    public Iterable<InputFileDetails> getAllFileChanges() {
        CollectingChangeVisitor visitor = new CollectingChangeVisitor();
        changes.accept(visitor);
        return Cast.uncheckedNonnullCast(visitor.getChanges());
    }
}
