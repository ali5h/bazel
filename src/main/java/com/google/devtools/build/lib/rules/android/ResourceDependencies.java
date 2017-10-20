// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.android;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.configuredtargets.RuleConfiguredTarget.Mode;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.AttributeMap;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.packages.RuleErrorConsumer;
import javax.annotation.Nullable;

/**
 * Represents a container for the {@link ResourceContainer}s for a given library. This is
 * abstraction simplifies the process of managing and exporting the direct and transitive resource
 * dependencies of an android rule, as well as providing type safety.
 *
 * <p>The transitive and direct dependencies are not guaranteed to be disjoint. If a
 * library is included in both the transitive and direct dependencies, it will appear twice. This
 * requires consumers to manage duplicated resources gracefully.
 */
@Immutable
public final class ResourceDependencies {
  /**
   * Contains all the transitive resources that are not generated by the direct ancestors of the
   * current rule.
   *
   * @deprecated We are migrating towards storing each type of Artifact in a different NestedSet.
   *     This should allow greater efficiency since we don't need to unroll this NestedSet to get a
   *     particular input. TODO (b/67996945): Complete this migration.
   */
  @Deprecated
  private final NestedSet<ResourceContainer> transitiveResourceContainers;

  /**
   * Contains all the direct dependencies of the current target. Since a given direct dependency can
   * act as a "forwarding" library, collecting all the direct resource from it's dependencies and
   * providing them as "direct" dependencies to maintain merge order, this uses a NestedSet to
   * properly maintain ordering and ease of merging.
   *
   * @deprecated Similarly to {@link #transitiveResourceContainers}, we are moving away from storing
   *     ResourceContainer objects here. TODO (b/67996945): Complete this migration.
   */
  @Deprecated
  private final NestedSet<ResourceContainer> directResourceContainers;

  /**
   * Transitive resource files for this target.
   *
   * We keep them separate from the {@code transitiveAssets} so that we can filter them.
   */
  private final NestedSet<Artifact> transitiveResources;

  /**
   * Transitive asset files for this target.
   */
  private final NestedSet<Artifact> transitiveAssets;

  private final NestedSet<Artifact> transitiveManifests;

  private final NestedSet<Artifact> transitiveAapt2RTxt;

  private final NestedSet<Artifact> transitiveSymbolsBin;

  private final NestedSet<Artifact> transitiveCompiledSymbols;

  private final NestedSet<Artifact> transitiveStaticLib;

  private final NestedSet<Artifact> transitiveRTxt;

  /** Whether the resources of the current rule should be treated as neverlink. */
  private final boolean neverlink;

  public static ResourceDependencies fromRuleResources(RuleContext ruleContext, boolean neverlink) {
    if (!hasResourceAttribute(ruleContext)) {
      return empty();
    }

    NestedSetBuilder<ResourceContainer> transitiveDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<ResourceContainer> directDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveResources = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAssets = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveManifests = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAapt2RTxt = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveSymbolsBin = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveCompiledSymbols = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveStaticLib = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveRTxt = NestedSetBuilder.naiveLinkOrder();
    extractFromAttributes(
        ImmutableList.of("resources"),
        ruleContext,
        transitiveDependencies,
        directDependencies,
        transitiveResources,
        transitiveAssets,
        transitiveManifests,
        transitiveAapt2RTxt,
        transitiveSymbolsBin,
        transitiveCompiledSymbols,
        transitiveStaticLib,
        transitiveRTxt);
    return new ResourceDependencies(
        neverlink,
        transitiveDependencies.build(),
        directDependencies.build(),
        transitiveResources.build(),
        transitiveAssets.build(),
        transitiveManifests.build(),
        transitiveAapt2RTxt.build(),
        transitiveSymbolsBin.build(),
        transitiveCompiledSymbols.build(),
        transitiveStaticLib.build(),
        transitiveRTxt.build());
  }

  public static ResourceDependencies fromRuleDeps(RuleContext ruleContext, boolean neverlink) {
    NestedSetBuilder<ResourceContainer> transitiveDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<ResourceContainer> directDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveResources = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAssets = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveManifests = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAapt2RTxt = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveSymbolsBin = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveCompiledSymbols = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveStaticLib = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveRTxt = NestedSetBuilder.naiveLinkOrder();
    extractFromAttributes(
        AndroidCommon.TRANSITIVE_ATTRIBUTES,
        ruleContext,
        transitiveDependencies,
        directDependencies,
        transitiveResources,
        transitiveAssets,
        transitiveManifests,
        transitiveAapt2RTxt,
        transitiveSymbolsBin,
        transitiveCompiledSymbols,
        transitiveStaticLib,
        transitiveRTxt);
    return new ResourceDependencies(
        neverlink,
        transitiveDependencies.build(),
        directDependencies.build(),
        transitiveResources.build(),
        transitiveAssets.build(),
        transitiveManifests.build(),
        transitiveAapt2RTxt.build(),
        transitiveSymbolsBin.build(),
        transitiveCompiledSymbols.build(),
        transitiveStaticLib.build(),
        transitiveRTxt.build());
  }

  public static ResourceDependencies fromRuleResourceAndDeps(RuleContext ruleContext,
      boolean neverlink) {
    NestedSetBuilder<ResourceContainer> transitiveDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<ResourceContainer> directDependencies = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveResources = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAssets = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveManifests = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveAapt2RTxt = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveSymbolsBin = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveCompiledSymbols = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveStaticLib = NestedSetBuilder.naiveLinkOrder();
    NestedSetBuilder<Artifact> transitiveRTxt = NestedSetBuilder.naiveLinkOrder();
    if (hasResourceAttribute(ruleContext)) {
      extractFromAttributes(
          ImmutableList.of("resources"),
          ruleContext,
          transitiveDependencies,
          directDependencies,
          transitiveResources,
          transitiveAssets,
          transitiveManifests,
          transitiveAapt2RTxt,
          transitiveSymbolsBin,
          transitiveCompiledSymbols,
          transitiveStaticLib,
          transitiveRTxt);
    }
    if (directDependencies.isEmpty()) {
      // There are no resources, so this library will forward the direct and transitive dependencies
      // without changes.
      extractFromAttributes(
          AndroidCommon.TRANSITIVE_ATTRIBUTES,
          ruleContext,
          transitiveDependencies,
          directDependencies,
          transitiveResources,
          transitiveAssets,
          transitiveManifests,
          transitiveAapt2RTxt,
          transitiveSymbolsBin,
          transitiveCompiledSymbols,
          transitiveStaticLib,
          transitiveRTxt);
    } else {
      // There are resources, so the direct dependencies and the transitive will be merged into
      // the transitive dependencies. This maintains the relationship of the resources being
      // directly on the rule.
      extractFromAttributes(
          AndroidCommon.TRANSITIVE_ATTRIBUTES,
          ruleContext,
          transitiveDependencies,
          transitiveDependencies,
          transitiveResources,
          transitiveAssets,
          transitiveManifests,
          transitiveAapt2RTxt,
          transitiveSymbolsBin,
          transitiveCompiledSymbols,
          transitiveStaticLib,
          transitiveRTxt);
    }
    return new ResourceDependencies(
        neverlink,
        transitiveDependencies.build(),
        directDependencies.build(),
        transitiveResources.build(),
        transitiveAssets.build(),
        transitiveManifests.build(),
        transitiveAapt2RTxt.build(),
        transitiveSymbolsBin.build(),
        transitiveCompiledSymbols.build(),
        transitiveStaticLib.build(),
        transitiveRTxt.build());
  }

  private static void extractFromAttributes(
      Iterable<String> attributeNames,
      RuleContext ruleContext,
      NestedSetBuilder<ResourceContainer> builderForTransitive,
      NestedSetBuilder<ResourceContainer> builderForDirect,
      NestedSetBuilder<Artifact> transitiveResources,
      NestedSetBuilder<Artifact> transitiveAssets,
      NestedSetBuilder<Artifact> transitiveManifests,
      NestedSetBuilder<Artifact> transitiveAapt2RTxt,
      NestedSetBuilder<Artifact> transitiveSymbolsBin,
      NestedSetBuilder<Artifact> transitiveCompiledSymbols,
      NestedSetBuilder<Artifact> transitiveStaticLib,
      NestedSetBuilder<Artifact> transitiveRTxt) {
    AttributeMap attributes = ruleContext.attributes();
    for (String attr : attributeNames) {
      if (!attributes.has(attr, BuildType.LABEL_LIST) && !attributes.has(attr, BuildType.LABEL)) {
        continue;
      }
      for (AndroidResourcesProvider resources :
          ruleContext.getPrerequisites(attr, Mode.TARGET, AndroidResourcesProvider.class)) {
        builderForTransitive.addTransitive(resources.getTransitiveAndroidResources());
        builderForDirect.addTransitive(resources.getDirectAndroidResources());
        transitiveResources.addTransitive(resources.getTransitiveResources());
        transitiveAssets.addTransitive(resources.getTransitiveAssets());
        transitiveManifests.addTransitive(resources.getTransitiveManifests());
        transitiveAapt2RTxt.addTransitive(resources.getTransitiveAapt2RTxt());
        transitiveSymbolsBin.addTransitive(resources.getTransitiveSymbolsBin());
        transitiveCompiledSymbols.addTransitive(resources.getTransitiveCompiledSymbols());
        transitiveStaticLib.addTransitive(resources.getTransitiveStaticLib());
        transitiveRTxt.addTransitive(resources.getTransitiveRTxt());
      }
    }
  }

  /**
   * Check for the existence of a "resources" attribute.
   *
   * <p>The existence of the resources attribute is not guaranteed on for all android rules, so it
   * is necessary to check for it.
   *
   * @param ruleContext The context to check.
   * @return True if the ruleContext has resources, otherwise, false.
   */
  private static boolean hasResourceAttribute(RuleContext ruleContext) {
    return ruleContext.attributes().has("resources", BuildType.LABEL);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("transitiveResourceContainers", transitiveResourceContainers)
        .add("directResourceContainers", directResourceContainers)
        .add("transitiveResources", transitiveResources)
        .add("transitiveAssets", transitiveAssets)
        .add("transitiveManifests", transitiveManifests)
        .add("transitiveAapt2RTxt", transitiveAapt2RTxt)
        .add("transitiveSymbolsBin", transitiveSymbolsBin)
        .add("transitiveCompiledSymbols", transitiveCompiledSymbols)
        .add("transitiveStaticLib", transitiveStaticLib)
        .add("transitiveRTxt", transitiveRTxt)
        .add("neverlink", neverlink)
        .toString();
  }

  /**
   * Creates an empty ResourceDependencies instance. This is used when an AndroidResources rule
   * is the only resource dependency. The most common case is the AndroidTest rule.
   */
  public static ResourceDependencies empty() {
    return new ResourceDependencies(
        false,
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER),
        NestedSetBuilder.emptySet(Order.NAIVE_LINK_ORDER));
  }

  private ResourceDependencies(
      boolean neverlink,
      NestedSet<ResourceContainer> transitiveResourceContainers,
      NestedSet<ResourceContainer> directResourceContainers,
      NestedSet<Artifact> transitiveResources,
      NestedSet<Artifact> transitiveAssets,
      NestedSet<Artifact> transitiveManifests,
      NestedSet<Artifact> transitiveAapt2RTxt,
      NestedSet<Artifact> transitiveSymbolsBin,
      NestedSet<Artifact> transitiveCompiledSymbols,
      NestedSet<Artifact> transitiveStaticLib,
      NestedSet<Artifact> transitiveRTxt) {
    this.neverlink = neverlink;
    this.transitiveResourceContainers = transitiveResourceContainers;
    this.directResourceContainers = directResourceContainers;
    this.transitiveResources = transitiveResources;
    this.transitiveAssets = transitiveAssets;
    this.transitiveManifests = transitiveManifests;
    this.transitiveAapt2RTxt = transitiveAapt2RTxt;
    this.transitiveSymbolsBin = transitiveSymbolsBin;
    this.transitiveCompiledSymbols = transitiveCompiledSymbols;
    this.transitiveStaticLib = transitiveStaticLib;
    this.transitiveRTxt = transitiveRTxt;
  }

  /**
   * Returns a copy of this instance with filtered resources. The original object is unchanged, and
   * may be returned if no filtering should be done.
   */
  public ResourceDependencies filter(RuleErrorConsumer ruleErrorConsumer, ResourceFilter filter) {
    NestedSet<Artifact> filteredResources =
        filter.filterDependencies(ruleErrorConsumer, transitiveResources);

    if (filteredResources == transitiveResources) {
      // No filtering was done.
      return this;
    }

    // Note that this doesn't filter any of the dependent artifacts. This
    // means that if any resource changes, the corresponding actions will get
    // re-executed
    return new ResourceDependencies(
        neverlink,
        filter.filterDependencyContainers(ruleErrorConsumer, transitiveResourceContainers),
        filter.filterDependencyContainers(ruleErrorConsumer, directResourceContainers),
        filteredResources,
        transitiveAssets,
        transitiveManifests,
        transitiveAapt2RTxt,
        transitiveSymbolsBin,
        transitiveCompiledSymbols,
        transitiveStaticLib,
        transitiveRTxt);
  }

  /**
   * Creates a new AndroidResourcesProvider with the supplied ResourceContainer as the direct dep.
   *
   * <p>When a library produces a new resource container the AndroidResourcesProvider should use
   * that container as a the direct dependency for that library. This makes the consuming rule to
   * identify the new container and merge appropriately. The previous direct dependencies are then
   * added to the transitive dependencies.
   *
   * @param label The label of the library exporting this provider.
   * @param newDirectResource The new direct dependency for AndroidResourcesProvider
   * @param isResourcesOnly if the direct dependency is either an android_resources target or an
   *     android_library target with no fields that android_resources targets do not provide.
   * @return A provider with the current resources and label.
   */
  public AndroidResourcesProvider toProvider(
      Label label, ResourceContainer newDirectResource, boolean isResourcesOnly) {
    if (neverlink) {
      return ResourceDependencies.empty().toProvider(label, isResourcesOnly);
    }
    return AndroidResourcesProvider.create(
        label,
        NestedSetBuilder.<ResourceContainer>naiveLinkOrder()
            .addTransitive(transitiveResourceContainers)
            .addTransitive(directResourceContainers)
            .build(),
        NestedSetBuilder.<ResourceContainer>naiveLinkOrder().add(newDirectResource).build(),
        NestedSetBuilder.<Artifact>naiveLinkOrder()
            .addTransitive(transitiveResources)
            .addAll(newDirectResource.getResources())
            .build(),
        NestedSetBuilder.<Artifact>naiveLinkOrder()
            .addTransitive(transitiveAssets)
            .addAll(newDirectResource.getAssets())
            .build(),
        withDirectAndTransitive(newDirectResource.getManifest(), transitiveManifests),
        withDirectAndTransitive(newDirectResource.getAapt2RTxt(), transitiveAapt2RTxt),
        withDirectAndTransitive(newDirectResource.getSymbols(), transitiveSymbolsBin),
        withDirectAndTransitive(newDirectResource.getCompiledSymbols(), transitiveCompiledSymbols),
        withDirectAndTransitive(newDirectResource.getStaticLibrary(), transitiveStaticLib),
        withDirectAndTransitive(newDirectResource.getRTxt(), transitiveRTxt),
        isResourcesOnly);
  }

  private static NestedSet<Artifact> withDirectAndTransitive(
      @Nullable Artifact direct, NestedSet<Artifact> transitive) {
    NestedSetBuilder<Artifact> builder = NestedSetBuilder.naiveLinkOrder();
    builder.addTransitive(transitive);
    if (direct != null) {
      builder.add(direct);
    }

    return builder.build();
  }

  /**
   * Create a new AndroidResourcesProvider from the dependencies of this library.
   *
   * <p>When a library doesn't export resources it should simply forward the current transitive and
   * direct resources to the consuming rule. This allows the consuming rule to make decisions about
   * the resource merging as if this library didn't exist.
   *
   * @param label The label of the library exporting this provider.
   * @param isResourcesOnly if the direct dependency is either an android_resources
   *     target or an android_library target with no fields that android_resources targets do not
   *     provide.
   * @return A provider with the current resources and label.
   */
  public AndroidResourcesProvider toProvider(Label label, boolean isResourcesOnly) {
    if (neverlink) {
      return ResourceDependencies.empty().toProvider(label, isResourcesOnly);
    }
    return AndroidResourcesProvider.create(
        label,
        transitiveResourceContainers,
        directResourceContainers,
        transitiveResources,
        transitiveAssets,
        transitiveManifests,
        transitiveAapt2RTxt,
        transitiveSymbolsBin,
        transitiveCompiledSymbols,
        transitiveStaticLib,
        transitiveRTxt,
        isResourcesOnly);
  }

  /**
   * Provides an NestedSet of the direct and transitive resources.
   *
   * @deprecated Rather than accessing the ResourceContainers, use other methods in this class to
   *     get the specific Artifacts you need instead.
   */
  @Deprecated
  public NestedSet<ResourceContainer> getResourceContainers() {
    return NestedSetBuilder.<ResourceContainer>naiveLinkOrder()
        .addTransitive(directResourceContainers)
        .addTransitive(transitiveResourceContainers)
        .build();
  }

  /**
   * @deprecated Rather than accessing the ResourceContainers, use other methods in this class to
   *     get the specific Artifacts you need instead.
   */
  @Deprecated
  public NestedSet<ResourceContainer> getTransitiveResourceContainers() {
    return transitiveResourceContainers;
  }

  /**
   * @deprecated Rather than accessing the ResourceContainers, use other methods in this class to
   *     get the specific Artifacts you need instead.
   */
  @Deprecated
  public NestedSet<ResourceContainer> getDirectResourceContainers() {
    return directResourceContainers;
  }

  public NestedSet<Artifact> getTransitiveResources() {
    return transitiveResources;
  }

  public NestedSet<Artifact> getTransitiveAssets() {
    return transitiveAssets;
  }

  public NestedSet<Artifact> getTransitiveManifests() {
    return transitiveManifests;
  }

  public NestedSet<Artifact> getTransitiveAapt2RTxt() {
    return transitiveAapt2RTxt;
  }

  public NestedSet<Artifact> getTransitiveSymbolsBin() {
    return transitiveSymbolsBin;
  }

  /**
   * @return The transitive closure of compiled symbols.
   * Compiled symbols are zip files containing the compiled resource output of aapt2 compile
   */
  public NestedSet<Artifact> getTransitiveCompiledSymbols() {
    return transitiveCompiledSymbols;
  }

  public NestedSet<Artifact> getTransitiveStaticLib() {
    return transitiveStaticLib;
  }

  public NestedSet<Artifact> getTransitiveRTxt() {
    return transitiveRTxt;
  }
}
