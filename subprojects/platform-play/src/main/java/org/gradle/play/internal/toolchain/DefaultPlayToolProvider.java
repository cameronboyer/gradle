/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.internal.toolchain;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.compile.daemon.CompilerDaemonManager;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.Factory;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.play.internal.coffeescript.CoffeeScriptCompileSpec;
import org.gradle.play.internal.coffeescript.CoffeeScriptCompiler;
import org.gradle.play.internal.routes.RoutesCompileSpec;
import org.gradle.play.internal.routes.RoutesCompileSpecFactory;
import org.gradle.play.internal.routes.RoutesCompiler;
import org.gradle.play.internal.routes.VersionedRoutesCompileSpec;
import org.gradle.play.internal.run.PlayApplicationRunner;
import org.gradle.play.internal.run.PlayRunSpec;
import org.gradle.play.internal.run.PlayRunSpecFactory;
import org.gradle.play.internal.run.VersionedPlayRunSpec;
import org.gradle.play.internal.twirl.TwirlCompileSpec;
import org.gradle.play.internal.twirl.TwirlCompileSpecFactory;
import org.gradle.play.internal.twirl.TwirlCompiler;
import org.gradle.play.internal.twirl.VersionedTwirlCompileSpec;
import org.gradle.play.platform.PlayPlatform;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.gradle.util.TreeVisitor;
import org.gradle.util.WrapUtil;

import java.io.File;
import java.util.Map;

class DefaultPlayToolProvider implements PlayToolProvider {
    private final FileResolver fileResolver;
    private final CompilerDaemonManager compilerDaemonManager;
    private final ConfigurationContainer configurationContainer;
    private final DependencyHandler dependencyHandler;
    private PlayPlatform targetPlatform;

    public DefaultPlayToolProvider(FileResolver fileResolver, CompilerDaemonManager compilerDaemonManager, ConfigurationContainer configurationContainer, DependencyHandler dependencyHandler,  PlayPlatform targetPlatform) {
        this.fileResolver = fileResolver;
        this.compilerDaemonManager = compilerDaemonManager;
        this.configurationContainer = configurationContainer;
        this.dependencyHandler = dependencyHandler;
        this.targetPlatform = targetPlatform;
    }

    public <T extends CompileSpec> org.gradle.language.base.internal.compile.Compiler<T> newCompiler(T spec) {
        if (spec instanceof TwirlCompileSpec) {
            TwirlCompileSpec twirlCompileSpec = (TwirlCompileSpec)spec;
            VersionedTwirlCompileSpec versionedSpec = TwirlCompileSpecFactory.create(twirlCompileSpec, targetPlatform);
            DaemonPlayCompiler<VersionedTwirlCompileSpec> compiler = new DaemonPlayCompiler<VersionedTwirlCompileSpec>(fileResolver.resolve("."), new TwirlCompiler(), compilerDaemonManager, resolveClasspath(versionedSpec.getDependencyNotation()));
            @SuppressWarnings("unchecked") Compiler<T> twirlCompileSpecCompiler = (Compiler<T>) new MappingSpecCompiler<TwirlCompileSpec, VersionedTwirlCompileSpec>(compiler, WrapUtil.toMap(twirlCompileSpec, versionedSpec));
            return twirlCompileSpecCompiler;
        } else if(spec instanceof RoutesCompileSpec){
            RoutesCompileSpec routesCompileSpec = (RoutesCompileSpec)spec;
            VersionedRoutesCompileSpec versionedSpec = RoutesCompileSpecFactory.create(routesCompileSpec, targetPlatform);
            DaemonPlayCompiler<VersionedRoutesCompileSpec> compiler = new DaemonPlayCompiler<VersionedRoutesCompileSpec>(fileResolver.resolve("."), new RoutesCompiler(), compilerDaemonManager, resolveClasspath(versionedSpec.getDependencyNotation()));
            @SuppressWarnings("unchecked") Compiler<T> routesSpecCompiler = (Compiler<T>) new MappingSpecCompiler<RoutesCompileSpec, VersionedRoutesCompileSpec>(compiler, WrapUtil.toMap(routesCompileSpec, versionedSpec));
            return routesSpecCompiler;
        } else if (spec instanceof CoffeeScriptCompileSpec) {
            // TODO This should probably be a DaemonCompiler but DaemonPlayCompiler requires a VersionedPlayCompileSpec
            @SuppressWarnings("unchecked") Compiler<T> coffeeScriptCompiler = (Compiler<T>) new CoffeeScriptCompiler();
            return coffeeScriptCompiler;
        }

        return null;
    }

    public PlayApplicationRunner newApplicationRunner(Factory<WorkerProcessBuilder> workerProcessBuilderFactory, PlayRunSpec spec) {
        VersionedPlayRunSpec versionedSpec = PlayRunSpecFactory.create(spec, targetPlatform);
        Iterable<File> docsClasspath = resolveClasspath(versionedSpec.getDocsDependencyNotation());
        return new PlayApplicationRunner(fileResolver.resolve("."), workerProcessBuilderFactory, versionedSpec, docsClasspath); //We pass docsClasspath here, but it could have been part of the VersionedPlayRunSpec, but we want to contain the resolution of dependencies in this file
    }



    private Iterable<File> resolveClasspath(Object dependencyNotation) {
        Dependency compilerDependency = dependencyHandler.create(dependencyNotation);
        return configurationContainer.detachedConfiguration(compilerDependency).getFiles();
    }

    public boolean isAvailable() {
        return true;
    }

    public void explain(TreeVisitor<? super String> visitor) {

    }

    private class MappingSpecCompiler<T extends CompileSpec, V extends T> implements Compiler<T>  {
        private Compiler<V> delegate;
        private final Map<T, V> mapping;

        public MappingSpecCompiler(Compiler<V> delegate, Map<T, V> mapping){
            this.delegate = delegate;
            this.mapping = mapping;
        }

        public WorkResult execute(T spec) {
            return delegate.execute(mapping.get(spec));
        }
    }
}
