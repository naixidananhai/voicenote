package com.voicelife.assistant.utils;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DebugLogger_Factory implements Factory<DebugLogger> {
  @Override
  public DebugLogger get() {
    return newInstance();
  }

  public static DebugLogger_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DebugLogger newInstance() {
    return new DebugLogger();
  }

  private static final class InstanceHolder {
    private static final DebugLogger_Factory INSTANCE = new DebugLogger_Factory();
  }
}
