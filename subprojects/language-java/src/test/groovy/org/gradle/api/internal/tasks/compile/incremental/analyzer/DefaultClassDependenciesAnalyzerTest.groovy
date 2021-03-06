/*
 * Copyright 2013 the original author or authors.
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


package org.gradle.api.internal.tasks.compile.incremental.analyzer

import org.gradle.api.internal.cache.StringInterner
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeClassAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeRuntimeAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeSourceAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesClassAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesRuntimeAnnotation
import org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.UsesSourceAnnotation
import org.gradle.api.internal.tasks.compile.incremental.deps.ClassAnalysis
import org.gradle.api.internal.tasks.compile.incremental.test.HasInnerClass
import org.gradle.api.internal.tasks.compile.incremental.test.HasNonPrivateConstants
import org.gradle.api.internal.tasks.compile.incremental.test.HasPrivateConstants
import org.gradle.api.internal.tasks.compile.incremental.test.HasPublicConstants
import org.gradle.api.internal.tasks.compile.incremental.test.SomeClass
import org.gradle.api.internal.tasks.compile.incremental.test.SomeOtherClass
import org.gradle.api.internal.tasks.compile.incremental.test.UsedByNonPrivateConstantsClass
import org.gradle.api.internal.tasks.compile.incremental.test.YetAnotherClass
import spock.lang.Specification
import spock.lang.Subject

class DefaultClassDependenciesAnalyzerTest extends Specification {

    @Subject
    analyzer = new DefaultClassDependenciesAnalyzer(new StringInterner())

    private ClassAnalysis analyze(Class foo) {
        analyzer.getClassAnalysis(classStream(foo))
    }

    def "knows the name of a class"() {
        expect:
        analyze(SomeOtherClass).className == SomeOtherClass.name
        analyze(HasInnerClass.InnerThing).className == HasInnerClass.InnerThing.name
    }

    def "knows dependencies of a java class"() {
        expect:
        analyze(SomeOtherClass).classDependencies == [YetAnotherClass.name, SomeClass.name] as Set
    }

    def "knows basic class dependencies of a groovy class"() {
        def deps = analyze(DefaultClassDependenciesAnalyzerTest).classDependencies

        expect:
        deps.contains(Specification.class.name)
    }

    def "knows if a class have non-private constants"() {
        when:
        def analysis = analyze(HasNonPrivateConstants)

        then:
        analysis.classDependencies == [UsedByNonPrivateConstantsClass.name] as Set
        !analysis.dependencyToAll
        analysis.constants == ['X|1'.hashCode()] as Set

        when:
        analysis = analyze(HasPublicConstants)

        then:
        analysis.classDependencies.isEmpty()
        !analysis.dependencyToAll
        analysis.constants == ['X|1'.hashCode()] as Set

        when:
        analysis = analyze(HasPrivateConstants)

        then:
        analysis.classDependencies == [HasNonPrivateConstants.name] as Set
        !analysis.dependencyToAll
        analysis.constants == [] as Set
    }

    def "knows if a class uses annotations with source retention"() {
        expect:
        analyze(UsesRuntimeAnnotation).classDependencies  == ["org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeRuntimeAnnotation"] as Set
        analyze(SomeRuntimeAnnotation).classDependencies.isEmpty()
        !analyze(SomeRuntimeAnnotation).dependencyToAll

        analyze(UsesClassAnnotation).classDependencies == ["org.gradle.api.internal.tasks.compile.incremental.analyzer.annotations.SomeClassAnnotation"] as Set
        analyze(SomeClassAnnotation).classDependencies.isEmpty()
        !analyze(SomeClassAnnotation).dependencyToAll

        analyze(UsesSourceAnnotation).classDependencies.isEmpty() //source annotations are wiped from the bytecode
        analyze(SomeSourceAnnotation).classDependencies.isEmpty()
        analyze(SomeSourceAnnotation).dependencyToAll
    }

    InputStream classStream(Class aClass) {
        aClass.classLoader.getResourceAsStream(aClass.getName().replace(".", "/") + ".class")
    }
}
